package com.avrapps.pdfviewer.tools_fragment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.crypto.BadPasswordException;
import com.itextpdf.kernel.pdf.EncryptionConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfIndirectReference;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfNumber;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfStream;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.ReaderProperties;
import com.itextpdf.kernel.pdf.WriterProperties;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import com.itextpdf.kernel.utils.PdfMerger;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class PDFUtilities {

    //https://github.com/Swati4star/Images-to-PDF/blob/1e855603f60631dc505f378cd6ba077eeb7dffda/app/src/main/java/swati4star/createpdf/util/PDFUtils.java#L105
    // https://kb.itextpdf.com/home/it7kb/examples/reduce-image
    public static void compressPdf(String source, String sourcePassword, String destination) throws Exception {

        PdfWriter writer = new PdfWriter(destination, new WriterProperties().setFullCompressionMode(true));
        PdfDocument pdfDoc = new PdfDocument(getPdfReader(source, sourcePassword), writer);
        int quality = 40;

        for (PdfIndirectReference indRef : pdfDoc.listIndirectReferences()) {

            // Get a direct object and try to resolve indirects chain.
            // Note: If chain of references has length of more than 32,
            // this method return 31st reference in chain.
            PdfObject pdfObject = indRef.getRefersTo();
            if (pdfObject == null || !pdfObject.isStream()) {
                continue;
            }

            PdfStream stream = (PdfStream) pdfObject;
            if (!PdfName.Image.equals(stream.getAsName(PdfName.Subtype))) {
                continue;
            }

            if (!PdfName.DCTDecode.equals(stream.getAsName(PdfName.Filter))) {
                continue;
            }

            PdfImageXObject image = new PdfImageXObject(stream);
            byte[] imageBytes = image.getImageBytes();
            Bitmap bmp;
            bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (bmp == null) return;

            int width = bmp.getWidth();
            int height = bmp.getHeight();

            Bitmap outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas outCanvas = new Canvas(outBitmap);
            outCanvas.drawBitmap(bmp, 0f, 0f, null);


            ByteArrayOutputStream imgBytes = new ByteArrayOutputStream();
            outBitmap.compress(Bitmap.CompressFormat.JPEG, quality, imgBytes);
            stream.clear();
            stream.setData(imgBytes.toByteArray());
            stream.put(PdfName.Type, PdfName.XObject);
            stream.put(PdfName.Subtype, PdfName.Image);
            stream.put(PdfName.Filter, PdfName.DCTDecode);
            stream.put(PdfName.Width, new PdfNumber(width));
            stream.put(PdfName.Height, new PdfNumber(height));
            stream.put(PdfName.BitsPerComponent, new PdfNumber(8));
            stream.put(PdfName.ColorSpace, PdfName.DeviceRGB);
        }

        pdfDoc.close();
    }

    private static PdfReader getPdfReader(String source, String sourcePassword) throws IOException {
        PdfReader pdfReader;
        if (sourcePassword == null || sourcePassword.isEmpty()) {
            pdfReader = new PdfReader(source);
        } else {
            ReaderProperties rp = new ReaderProperties();
            pdfReader = new PdfReader(source, rp.setPassword(sourcePassword.getBytes()));
        }
        return pdfReader;
    }

    //https://kb.itextpdf.com/home/it7kb/examples/encrypting-decrypting-pdfs
    public static void setPasswordToPDF(String source, String sourcePassword, String destination, String ownerPassword, String userPassword) throws Exception {
        PdfDocument pdfDoc = new PdfDocument(
                getPdfReader(source, sourcePassword),
                new PdfWriter(destination, new WriterProperties().setStandardEncryption(
                        userPassword.getBytes(),
                        ownerPassword.getBytes(),
                        EncryptionConstants.ALLOW_PRINTING,
                        EncryptionConstants.ENCRYPTION_AES_128 | EncryptionConstants.DO_NOT_ENCRYPT_METADATA))
        );
        pdfDoc.close();
    }

    public static void removePasswordFromPDF(String source, String destination, String ownerPassword) throws Exception {
        try (PdfDocument document = new PdfDocument(
                new PdfReader(source, new ReaderProperties().setPassword(ownerPassword.getBytes())),
                new PdfWriter(destination)
        )) {
            byte[] userPasswordBytes = document.getReader().computeUserPassword();
            // The result of user password computation logic can be null in case of AES256 password encryption or non password encryption algorithm
            String userPassword = userPasswordBytes == null ? null : new String(userPasswordBytes);
            System.out.println(userPassword);
        }
    }

    public static void deletePages(String source, String sourcePassword, String destination, ArrayList<Integer> pages) throws Exception {
        PdfReader reader = getPdfReader(source, sourcePassword);
        PdfWriter writer = new PdfWriter(destination);
        PdfDocument document = new PdfDocument(reader, writer);
        for (Integer page : pages) {
            document.removePage(page);
        }
        document.close();
    }

    public static void imagesToPdf(HashMap<String, String> selectedFiles, String destinationFile) throws Exception {
        File file = new File(destinationFile);

        PdfWriter pdfWriter = new PdfWriter(file);
        PdfDocument pdfDocument = new PdfDocument(pdfWriter);

        Document document = new Document(pdfDocument);
        for (String selectedFile : selectedFiles.keySet()) {
            ImageData imageData = ImageDataFactory.create(selectedFile);
            Image pdfImg = new Image(imageData);
            document.add(pdfImg);
        }
        document.close();
    }

    public static void mergePdfFiles(HashMap<String, String> selectedFiles, String destinationFile) throws Exception {
        PdfDocument pdf = new PdfDocument(new PdfWriter(destinationFile));
        PdfMerger merger = new PdfMerger(pdf);
        for (String selectedFile : selectedFiles.keySet()) {
            PdfReader reader = getPdfReader(selectedFile, selectedFiles.get(selectedFile));
            PdfDocument pdfDocument = new PdfDocument(reader);
            merger.merge(pdfDocument, 1, pdfDocument.getNumberOfPages());
            pdfDocument.close();
        }
        pdf.close();
    }

    public static boolean isPasswordProtected(String pdfPath) {
        try {
            try {
                PdfReader reader = new PdfReader(pdfPath);
                new PdfDocument(reader);
                return reader.isEncrypted();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        } catch (BadPasswordException ex) {
            return true;
        }
    }
    public static boolean isPasswordValid(String pdfPath, String password) {
        try {
            ReaderProperties properties = new ReaderProperties();
            properties.setPassword(password.getBytes());
            try {
                PdfReader reader = new PdfReader(pdfPath, properties);
                new PdfDocument(reader);
                return reader.isOpenedWithFullPermission();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        } catch (BadPasswordException ex) {
            return false;
        }
    }

}
