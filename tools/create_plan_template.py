from pathlib import Path
from docx import Document
from docx.shared import Cm, Pt, RGBColor
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

out=Path('output/templates');out.mkdir(parents=True,exist_ok=True)
doc=Document();section=doc.sections[0]
section.page_width=Cm(21);section.page_height=Cm(29.7)
section.top_margin=Cm(1.5);section.bottom_margin=Cm(1.5);section.left_margin=Cm(2);section.right_margin=Cm(2)
for name,size in [('Normal',10),('Title',24),('Heading 1',16),('Heading 2',12)]:
    st=doc.styles[name];st.font.name='Microsoft YaHei';st.font.size=Pt(size);st.font.color.rgb=RGBColor(0,0,0)
    st.element.get_or_add_rPr().rFonts.set(qn('w:eastAsia'),'Microsoft YaHei')
    st.paragraph_format.space_after=Pt(4);st.paragraph_format.space_before=Pt(4);st.paragraph_format.line_spacing=1.05
doc.styles['Normal'].paragraph_format.space_after=Pt(3)
for style in doc.styles:
    for border in style.element.xpath('.//w:pBdr'):
        border.getparent().remove(border)
header=section.header.paragraphs[0];header.text='循迹    健身计划与打卡模板    V1'
header.style=doc.styles['Normal'];header.runs[0].font.size=Pt(9)
footer=section.footer.paragraphs[0];footer.text='循迹标准模板  |  计划只填写一次  每天按训练日打卡    '
fld=OxmlElement('w:fldSimple');fld.set(qn('w:instr'),'PAGE');footer._p.append(fld)
for r in footer.runs:r.font.size=Pt(8)

def para(text,style=None):return doc.add_paragraph(text,style)
def table(headers,rows,widths=None):
    t=doc.add_table(rows=1,cols=len(headers));t.style='Table Grid'
    for c,h in zip(t.rows[0].cells,headers):
        c.text=h;shd=OxmlElement('w:shd');shd.set(qn('w:fill'),'EAF0E8');c._tc.get_or_add_tcPr().append(shd)
        for r in c.paragraphs[0].runs:r.bold=True
    for values in rows:
        cells=t.add_row().cells
        for c,value in zip(cells,values):c.text=value
    for row in t.rows:
        pr=row._tr.get_or_add_trPr();pr.append(OxmlElement('w:cantSplit'))
        for c in row.cells:
            for p in c.paragraphs:
                p.paragraph_format.space_after=Pt(2);p.paragraph_format.space_before=Pt(2)
                for r in p.runs:r.font.size=Pt(10)
    # Keep spacing on the next heading instead of inserting blank paragraphs.
    return t

para('健身计划与每日打卡模板','Title')
para('先在第二页填写各训练日的项目，再把 Word 或导出的文字版 PDF 上传到循迹。App 会将项目保存为训练日模板；每天选择一次训练日，即可显示完整计划并打卡。')
para('基本信息','Heading 1')
table(['栏目','填写内容'],[['计划名称','____________________________'],['计划周期','从 __________ 至 __________'],['每周训练频率','______ 次  可选三练 四练 五练或其他频率'],['训练目标及备注','________________________________________']])
para('每周安排参考','Heading 1')
para('下表仅帮助安排时间，App 以你每天实际选择的训练日为准。训练日名称可自定义，同一训练日可在一周内重复使用。')
table(['星期','一','二','三','四','五','六','日'],[['训练日','______','______','______','______','______','______','______']])
para('三练可使用力量 A、力量 B、有氧三个训练日；四练可额外设置加练；五练可增加力量 C 或重复已有训练日。这里只说明结构，不指定训练内容。')
para('填写与上传','Heading 1')
para('1  在 Word 第二页替换花括号中的内容，删除未使用的项目行和训练日区块。需要更多训练日时，复制完整区块并改名。')
para('2  保留 DAY、ITEM 和结束标记的原样及顺序。每个 ITEM 段落对应一个项目；保留竖线分隔，内容可以换行。')
para('3  保存 .docx，或导出可选择文字的 PDF；上传后核对训练日名称和项目数量，再确认导入。扫描件和照片暂不支持。')
para('4  第三页可作纸面打卡。App 内直接填写三餐与加餐；纸面日记和第一页面的周安排不自动导入。')

doc.add_page_break();para('训练日计划填写区','Title')
para('每个区块只填写一次。项目字段依次为动作名称、计划组次或时长、负重、休息、备注；不适用的字段填写“无”。花括号占位行不会导入。')
for name in ['力量 A','力量 B','有氧日','加练']:
    para(name,'Heading 2')
    para('[DAY] '+name)
    for i in range(2):para('[ITEM] {动作名称} | {组次或时长} | {负重} | {休息} | {备注}')
    para('[/DAY]')
para('休息日','Heading 2');para('App 内已提供休息日选项，无需填写项目。')
para('填写示例','Heading 2');para('将一行占位文字改成你原有计划中的内容，例如“动作名称 | 3组8次 | 自选 | 90秒 | 备注”。实际项目须使用你自己的计划；示例本身不作为项目导入。')

doc.add_page_break();para('每日打卡记录','Title')
para('本页可复制或打印重复使用。App 中选择训练日后可逐项勾选，也可一键完成；实际组次、负重与感受均可选填。')
table(['日期','今日训练日','完成情况'],[['____________','________________','________________']])
para('训练实际记录','Heading 1')
table(['项目','实际组次或时长','负重与备注'],[['________________','________________','________________'] for _ in range(4)])
para('三餐与加餐','Heading 1')
table(['餐次','食物与份量','热量 kcal 选填'],[[meal,'________________________','________'] for meal in ['早餐','午餐','晚餐','加餐']])
para('身体指标','Heading 1')
table(['体重 kg','体脂率 % 选填','腰围 cm 选填'],[['________','________','________']])
para('当日感受与备注','Heading 1');para('________________________________________________________________')
para('________________________________________________________________')
doc.save(out/'fitness-plan.docx')
print(out/'fitness-plan.docx')
