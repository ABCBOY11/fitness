// Back up the formal app, replace its APK, and verify durable files before launch.
// Usage: node tools/safe_upgrade.mjs <adb.exe> <apk|--backup-only>
// This script never uninstalls an app or clears data.
import {execFileSync} from 'node:child_process';
import {createHash} from 'node:crypto';
import {mkdirSync,writeFileSync,existsSync} from 'node:fs';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';

const [adb,apk]=process.argv.slice(2);
if(!adb||!apk||(apk!=='--backup-only'&&!existsSync(apk)))throw Error('Supply adb.exe and an existing APK, or --backup-only');
const pkg='com.zzz.fitness';
const run=(...args)=>execFileSync(adb,args,{maxBuffer:1024*1024*1024,windowsHide:true});
function snapshot(){
    const archive=run('exec-out','run-as',pkg,'tar','-cf','-','.');
    const manifest={};
    for(let offset=0;offset+512<=archive.length;){
        const header=archive.subarray(offset,offset+512);
        if(header.every(b=>b===0))break;
        const str=(start,end)=>header.subarray(start,end).toString().replace(/\0.*$/s,'');
        const prefix=str(345,500),name=(prefix?prefix+'/':'')+str(0,100);
        const path=name.replace(/^\.\//,'');
        const size=parseInt(str(124,136).trim()||'0',8);
        if(!Number.isSafeInteger(size)||size<0||offset+512+size>archive.length)throw Error('Invalid backup archive');
        const type=str(156,157);
        if(['databases','files','shared_prefs'].includes(path.split('/')[0])&&type!=='5'){
            if(type!==''&&type!=='0')throw Error('Unsupported backup entry: '+path);
            manifest[path]=createHash('sha256').update(archive.subarray(offset+512,offset+512+size)).digest('hex');
        }
        offset+=512+Math.ceil(size/512)*512;
    }
    if(archive.length<1024)throw Error('Backup archive is empty or incomplete');
    return {archive,manifest:Object.fromEntries(Object.entries(manifest).sort(([a],[b])=>a.localeCompare(b)))};
}
if(!run('shell','pm','path',pkg).toString().trim().startsWith('package:'))throw Error('Formal app is not installed; refusing to change state');
run('shell','am','force-stop',pkg);
const before=snapshot();
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const folder=resolve(root,'output/backups',new Date().toISOString().replace(/[:.]/g,'-'));
mkdirSync(folder,{recursive:true});
writeFileSync(resolve(folder,'app-private.tar'),before.archive);
writeFileSync(resolve(folder,'manifest.json'),JSON.stringify(before.manifest,null,2));
console.log(`Backup saved: ${folder}; durable files: ${Object.keys(before.manifest).length}`);
if(apk!=='--backup-only'){
    console.log(run('install','-r',resolve(apk)).toString().trim());
    const after=snapshot();
    writeFileSync(resolve(folder,'after-manifest.json'),JSON.stringify(after.manifest,null,2));
    if(JSON.stringify(before.manifest)!==JSON.stringify(after.manifest))throw Error('Data changed during replacement. Backup retained; app has not been launched: '+folder);
    console.log('Verified: database, plan documents and preferences are byte-for-byte unchanged.');
}
