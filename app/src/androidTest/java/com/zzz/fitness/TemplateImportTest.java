package com.zzz.fitness;
import android.content.Context;
import android.graphics.pdf.PdfRenderer;
import android.graphics.pdf.content.PdfPageTextContent;
import android.os.ParcelFileDescriptor;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class TemplateImportTest {
    private File fixture(String name)throws Exception{
        Context test=InstrumentationRegistry.getInstrumentation().getContext();File file=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),"test-"+name);
        try(InputStream in=test.getAssets().open(name);OutputStream out=new FileOutputStream(file)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}return file;
    }
    @Test public void filledWordAndPdfYieldSameFourTrainingDays()throws Exception{
        File word=fixture("filled-plan.docx"),pdf=fixture("filled-plan.pdf");
        try{
            List<TrainingPlan.Day> fromWord=TrainingPlan.parse(MainActivity.readDocx(word));StringBuilder text=new StringBuilder();
            try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(pdf,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer renderer=new PdfRenderer(fd)){
                for(int i=0;i<renderer.getPageCount();i++)try(PdfRenderer.Page page=renderer.openPage(i)){for(PdfPageTextContent t:page.getTextContents())text.append(t.getText()).append('\n');}
            }
            List<TrainingPlan.Day> fromPdf=TrainingPlan.parse(text.toString());assertEquals(4,fromWord.size());assertEquals(4,fromPdf.size());
            for(int i=0;i<4;i++){assertEquals(fromWord.get(i).name,fromPdf.get(i).name);assertEquals(2,fromPdf.get(i).items.size());}
        }finally{word.delete();pdf.delete();}
    }
}
