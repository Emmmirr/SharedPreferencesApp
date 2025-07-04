package com.example.sharedpreferencesapp;

import android.content.Context;
import android.util.Log;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PDFGenerator {
    private static final String TAG = "PDFGenerator";
    private Context context;
    private FileManager fileManager;

    public PDFGenerator(Context context) {
        this.context = context;
        this.fileManager = new FileManager(context);
    }

    public File generarPDFProtocolo(JSONObject protocolo) {
        try {
            // Crear archivo en memoria interna
            String nombreProyecto = protocolo.optString("nombreProyecto", "Protocolo");
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String nombreArchivo = "Protocolo_" + nombreProyecto.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp + ".pdf";

            File pdfFile = new File(context.getFilesDir(), nombreArchivo);

            PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            try {
                PdfFont font = PdfFontFactory.createFont();
                PdfFont boldFont = PdfFontFactory.createFont();

                // Encabezado institucional
                agregarEncabezado(document, boldFont);

                // Información del proyecto
                agregarSeccionProyecto(document, protocolo, font, boldFont);

                // Información del alumno
                agregarSeccionAlumno(document, protocolo, font, boldFont);

                // Información de la empresa
                agregarSeccionEmpresa(document, protocolo, font, boldFont);

            } finally {
                document.close();
            }

            Log.d(TAG, "PDF generado: " + pdfFile.getAbsolutePath());
            return pdfFile;

        } catch (Exception e) {
            Log.e(TAG, "Error generando PDF", e);
            return null;
        }
    }

    private void agregarEncabezado(Document document, PdfFont boldFont) throws Exception {
        Paragraph titulo = new Paragraph("INSTITUTO TECNOLÓGICO DE LOS MOCHIS")
                .setFont(boldFont)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(titulo);

        Paragraph division = new Paragraph("DIVISIÓN DE ESTUDIOS PROFESIONALES\nRESIDENCIA PROFESIONAL\nSOLICITUD DE RESIDENCIA PROFESIONAL")
                .setFont(boldFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(division);
    }

    private void agregarSeccionProyecto(Document document, JSONObject protocolo, PdfFont font, PdfFont boldFont) throws Exception {
        // Tabla de información básica
        Table tablaBasica = new Table(UnitValue.createPercentArray(new float[]{20, 30, 20, 30}));
        tablaBasica.setWidth(UnitValue.createPercentValue(100));

        String fecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        tablaBasica.addCell(crearCelda("Lugar:", boldFont));
        tablaBasica.addCell(crearCelda("", font));
        tablaBasica.addCell(crearCelda("Fecha:", boldFont));
        tablaBasica.addCell(crearCelda(fecha, font));

        tablaBasica.addCell(crearCelda("C:", boldFont));
        tablaBasica.addCell(crearCelda("", font));
        tablaBasica.addCell(crearCelda("ATN. C:", boldFont));
        tablaBasica.addCell(crearCelda("", font));

        document.add(tablaBasica);
        document.add(new Paragraph("\n"));

        // Información del proyecto
        Paragraph jefe = new Paragraph("Jefe de la Div. de Estudios Profesionales     Coord. de la Carrera de")
                .setFont(boldFont)
                .setFontSize(10)
                .setMarginBottom(10);
        document.add(jefe);

        Table tablaProyecto = new Table(UnitValue.createPercentArray(new float[]{25, 75}));
        tablaProyecto.setWidth(UnitValue.createPercentValue(100));

        tablaProyecto.addCell(crearCelda("NOMBRE DEL PROYECTO:", boldFont));
        tablaProyecto.addCell(crearCelda(protocolo.optString("nombreProyecto", ""), font));

        tablaProyecto.addCell(crearCelda("OPCIÓN ELEGIDA:", boldFont));
        tablaProyecto.addCell(crearCelda("Banco de Proyectos: " + protocolo.optString("banco", ""), font));

        document.add(tablaProyecto);
        document.add(new Paragraph("\n"));
    }

    private void agregarSeccionAlumno(Document document, JSONObject protocolo, PdfFont font, PdfFont boldFont) throws Exception {
        // Obtener datos del alumno
        JSONObject alumno = fileManager.buscarAlumnoPorId(protocolo.optString("alumnoId", ""));

        Paragraph datosResidente = new Paragraph("Datos del Residente:")
                .setFont(boldFont)
                .setFontSize(12)
                .setMarginBottom(10);
        document.add(datosResidente);

        Table tablaAlumno = new Table(UnitValue.createPercentArray(new float[]{20, 40, 20, 20}));
        tablaAlumno.setWidth(UnitValue.createPercentValue(100));

        if (alumno != null) {
            tablaAlumno.addCell(crearCelda("Nombre:", boldFont));
            tablaAlumno.addCell(crearCelda(alumno.optString("nombre", ""), font));
            tablaAlumno.addCell(crearCelda("Sexo:", boldFont));
            tablaAlumno.addCell(crearCelda(alumno.optString("sexo", ""), font));

            tablaAlumno.addCell(crearCelda("Carrera:", boldFont));
            tablaAlumno.addCell(crearCelda(alumno.optString("carrera", ""), font));
            tablaAlumno.addCell(crearCelda("No. de control:", boldFont));
            tablaAlumno.addCell(crearCelda(alumno.optString("numControl", ""), font));

            tablaAlumno.addCell(crearCelda("Domicilio:", boldFont));
            tablaAlumno.addCell(crearCelda(alumno.optString("direccion", ""), font));
            tablaAlumno.addCell(crearCelda("E-mail:", boldFont));
            tablaAlumno.addCell(crearCelda(alumno.optString("email", ""), font));

            tablaAlumno.addCell(crearCelda("Ciudad:", boldFont));
            tablaAlumno.addCell(crearCelda(protocolo.optString("ciudad", ""), font));
            tablaAlumno.addCell(crearCelda("Teléfono:", boldFont));
            tablaAlumno.addCell(crearCelda(alumno.optString("telefono", ""), font));
        }

        document.add(tablaAlumno);
        document.add(new Paragraph("\n"));
    }

    private void agregarSeccionEmpresa(Document document, JSONObject protocolo, PdfFont font, PdfFont boldFont) throws Exception {
        Paragraph datosEmpresa = new Paragraph("Datos de la institución o empresa donde realizará la residencia profesional:")
                .setFont(boldFont)
                .setFontSize(12)
                .setMarginBottom(10);
        document.add(datosEmpresa);

        Table tablaEmpresa = new Table(UnitValue.createPercentArray(new float[]{20, 30, 20, 30}));
        tablaEmpresa.setWidth(UnitValue.createPercentValue(100));

        tablaEmpresa.addCell(crearCelda("Nombre:", boldFont));
        tablaEmpresa.addCell(crearCelda(protocolo.optString("nombreEmpresa", ""), font));
        tablaEmpresa.addCell(crearCelda("R.F.C:", boldFont));
        tablaEmpresa.addCell(crearCelda(protocolo.optString("rfc", ""), font));

        String giroCompleto = protocolo.optString("giro", "");
        tablaEmpresa.addCell(crearCelda("Giro, Ramo o Sector:", boldFont));
        tablaEmpresa.addCell(crearCelda(giroCompleto, font));
        tablaEmpresa.addCell(crearCelda("", font));
        tablaEmpresa.addCell(crearCelda("", font));

        tablaEmpresa.addCell(crearCelda("Domicilio:", boldFont));
        tablaEmpresa.addCell(crearCelda(protocolo.optString("domicilio", ""), font));
        tablaEmpresa.addCell(crearCelda("Colonia:", boldFont));
        tablaEmpresa.addCell(crearCelda(protocolo.optString("colonia", ""), font));

        tablaEmpresa.addCell(crearCelda("Ciudad:", boldFont));
        tablaEmpresa.addCell(crearCelda(protocolo.optString("ciudad", ""), font));
        tablaEmpresa.addCell(crearCelda("C.P:", boldFont));
        tablaEmpresa.addCell(crearCelda(protocolo.optString("codigoPostal", ""), font));

        tablaEmpresa.addCell(crearCelda("Teléfono:", boldFont));
        tablaEmpresa.addCell(crearCelda(protocolo.optString("celular", ""), font));
        tablaEmpresa.addCell(crearCelda("Fax:", boldFont));
        tablaEmpresa.addCell(crearCelda("", font));

        // ⬅️ CORRECCIÓN: Misión en tabla separada para evitar colspan
        document.add(tablaEmpresa);
        document.add(new Paragraph("\n"));

        // Tabla separada para la misión
        Table tablaMision = new Table(UnitValue.createPercentArray(new float[]{25, 75}));
        tablaMision.setWidth(UnitValue.createPercentValue(100));

        tablaMision.addCell(crearCelda("Misión de la Empresa:", boldFont));
        tablaMision.addCell(crearCelda(protocolo.optString("mision", ""), font));

        document.add(tablaMision);
        document.add(new Paragraph("\n"));

        // Información de contactos
        Table tablaContactos = new Table(UnitValue.createPercentArray(new float[]{30, 40, 30}));
        tablaContactos.setWidth(UnitValue.createPercentValue(100));

        tablaContactos.addCell(crearCelda("Nombre del Titular:", boldFont));
        tablaContactos.addCell(crearCelda(protocolo.optString("titular", ""), font));
        tablaContactos.addCell(crearCelda("Puesto:", boldFont));

        tablaContactos.addCell(crearCelda("Nombre del Asesor Externo:", boldFont));
        tablaContactos.addCell(crearCelda(protocolo.optString("asesor", ""), font));
        tablaContactos.addCell(crearCelda("Puesto:", boldFont));

        tablaContactos.addCell(crearCelda("Nombre de la persona que firmará el Convenio:", boldFont));
        tablaContactos.addCell(crearCelda(protocolo.optString("firmante", ""), font));
        tablaContactos.addCell(crearCelda("Puesto:", boldFont));

        document.add(tablaContactos);
    }

    private Cell crearCelda(String texto, PdfFont font) throws Exception {
        return new Cell().add(new Paragraph(texto).setFont(font).setFontSize(9))
                .setPadding(3);
    }
}