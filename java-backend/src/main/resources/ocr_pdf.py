import sys
import io
import fitz
import easyocr
import numpy as np
from PIL import Image

# Ensure UTF-8 output on Windows
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

def ocr_pdf(pdf_path):
    doc = fitz.open(pdf_path)
    reader = easyocr.Reader(['ru', 'en'], gpu=False, verbose=False)
    all_text = []
    for i in range(doc.page_count):
        page = doc.load_page(i)
        pix = page.get_pixmap(dpi=300)
        img_bytes = pix.tobytes('png')
        img = Image.open(io.BytesIO(img_bytes))
        arr = np.array(img)
        result = reader.readtext(arr, detail=0, paragraph=True)
        all_text.append('\n'.join(result))
    doc.close()
    return '\n'.join(all_text)

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Usage: python ocr_pdf.py <pdf_path>", file=sys.stderr)
        sys.exit(1)
    pdf_path = sys.argv[1]
    try:
        text = ocr_pdf(pdf_path)
        print(text)
    except Exception as e:
        print(f"OCR error: {e}", file=sys.stderr)
        sys.exit(1)
