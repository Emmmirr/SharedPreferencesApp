package com.example.sharedpreferencesapp;

import android.content.Context;
import android.util.Log;

import com.itextpdf.io.font.constants.StandardFonts;
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

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class PDFGeneratorTramites {
    private static final String TAG = "PDFGeneratorTramites";
    private Context context;

    // Datos institucionales predefinidos
    private static final String LUGAR = "CHILPANCINGO DE LOS BRAVO, GUERRERO";
    private static final String JEFE_DIVISION = "M.A. MOISES VAZQUEZ PENA";

    public PDFGeneratorTramites(Context context) {
        this.context = context;
    }

    /**
     * Genera un PDF de trámite en un OutputStream
     */
    public boolean generarPDFTramiteEnOutputStream(String tipoDocumento, Map<String, String> datos, JSONObject alumno, OutputStream outputStream) {
        try {
            if (outputStream == null) {
                Log.e(TAG, "El OutputStream proporcionado es nulo.");
                return false;
            }

            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            try {
                PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA, "UTF-8");
                PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD, "UTF-8");

                agregarEncabezado(document, boldFont, tipoDocumento);
                agregarDatosAlumno(document, alumno, font, boldFont);
                agregarContenidoDocumento(document, tipoDocumento, datos, font, boldFont);
                agregarPiePagina(document, font);

            } finally {
                document.close();
            }

            Log.d(TAG, "PDF de trámite generado exitosamente.");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error generando PDF de trámite", e);
            return false;
        }
    }

    private void agregarEncabezado(Document document, PdfFont boldFont, String tipoDocumento) throws Exception {
        Paragraph titulo = new Paragraph("INSTITUTO TECNOLOGICO DE CHILPANCINGO")
                .setFont(boldFont)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(titulo);

        Paragraph division = new Paragraph("DIVISION DE ESTUDIOS PROFESIONALES\nRESIDENCIA PROFESIONAL")
                .setFont(boldFont)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(division);

        Paragraph tipoDoc = new Paragraph(obtenerTituloDocumento(tipoDocumento))
                .setFont(boldFont)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(tipoDoc);
    }

    private String obtenerTituloDocumento(String tipoDocumento) {
        switch (tipoDocumento) {
            case "solicitud_residencias":
                return "SOLICITUD DE RESIDENCIAS PROFESIONALES";
            case "carta_presentacion":
                return "CARTA DE PRESENTACIÓN";
            case "carta_aceptacion":
                return "CARTA DE ACEPTACIÓN";
            case "asignacion_asesor":
                return "ASIGNACIÓN DE ASESOR INTERNO";
            case "cronograma":
                return "CRONOGRAMA DE RESIDENCIA PROFESIONAL";
            case "formato_evaluacion":
                return "FORMATO DE EVALUACIÓN Y SEGUIMIENTO";
            case "reportes_parciales":
                return "REPORTES PARCIALES";
            case "carta_termino":
                return "CARTA DE TÉRMINO";
            case "acta_calificacion":
                return "ACTA DE CALIFICACIÓN";
            default:
                return "DOCUMENTO DE TRÁMITE";
        }
    }

    private void agregarDatosAlumno(Document document, JSONObject alumno, PdfFont font, PdfFont boldFont) throws Exception {
        if (alumno == null) return;

        Paragraph datosResidente = new Paragraph("DATOS DEL RESIDENTE:")
                .setFont(boldFont)
                .setFontSize(12)
                .setMarginBottom(10);
        document.add(datosResidente);

        Table tablaAlumno = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
        tablaAlumno.setWidth(UnitValue.createPercentValue(100));

        tablaAlumno.addCell(crearCelda("NOMBRE:", boldFont));
        tablaAlumno.addCell(crearCelda(convertirAMayusculasSinAcentos(alumno.optString("fullName", "")), font));

        tablaAlumno.addCell(crearCelda("NO. DE CONTROL:", boldFont));
        tablaAlumno.addCell(crearCelda(convertirAMayusculasSinAcentos(alumno.optString("controlNumber", "")), font));

        tablaAlumno.addCell(crearCelda("CARRERA:", boldFont));
        tablaAlumno.addCell(crearCelda(convertirAMayusculasSinAcentos(alumno.optString("career", "")), font));

        tablaAlumno.addCell(crearCelda("E-MAIL:", boldFont));
        tablaAlumno.addCell(crearCelda(alumno.optString("email", ""), font));

        document.add(tablaAlumno);
        document.add(new Paragraph("\n"));
    }

    private void agregarContenidoDocumento(Document document, String tipoDocumento, Map<String, String> datos, PdfFont font, PdfFont boldFont) throws Exception {
        // Tabla básica con lugar y fecha
        Table tablaBasica = new Table(UnitValue.createPercentArray(new float[]{20, 30, 20, 30}));
        tablaBasica.setWidth(UnitValue.createPercentValue(100));
        String fechaActual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        tablaBasica.addCell(crearCelda("LUGAR:", boldFont));
        tablaBasica.addCell(crearCelda(LUGAR, font));
        tablaBasica.addCell(crearCelda("FECHA:", boldFont));
        tablaBasica.addCell(crearCelda(fechaActual, font));
        document.add(tablaBasica);
        document.add(new Paragraph("\n"));

        // Contenido específico según el tipo de documento
        switch (tipoDocumento) {
            case "solicitud_residencias":
                agregarContenidoSolicitud(document, datos, font, boldFont);
                break;
            case "carta_presentacion":
                agregarContenidoCartaPresentacion(document, datos, font, boldFont);
                break;
            case "carta_aceptacion":
                agregarContenidoCartaAceptacion(document, datos, font, boldFont);
                break;
            default:
                agregarContenidoGenerico(document, datos, font, boldFont);
                break;
        }
    }

    private void agregarContenidoSolicitud(Document document, Map<String, String> datos, PdfFont font, PdfFont boldFont) throws Exception {
        Paragraph texto = new Paragraph("Por medio de la presente, solicito formalmente la autorización para realizar mi residencia profesional en:")
                .setFont(font)
                .setFontSize(10)
                .setMarginBottom(10);
        document.add(texto);

        if (datos.containsKey("nombreEmpresa")) {
            Table tabla = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
            tabla.setWidth(UnitValue.createPercentValue(100));
            tabla.addCell(crearCelda("EMPRESA:", boldFont));
            tabla.addCell(crearCelda(convertirAMayusculasSinAcentos(datos.get("nombreEmpresa")), font));
            document.add(tabla);
        }

        if (datos.containsKey("motivo")) {
            Paragraph motivo = new Paragraph("MOTIVO:")
                    .setFont(boldFont)
                    .setFontSize(10)
                    .setMarginTop(10);
            document.add(motivo);
            Paragraph textoMotivo = new Paragraph(convertirAMayusculasSinAcentos(datos.get("motivo")))
                    .setFont(font)
                    .setFontSize(10)
                    .setMarginBottom(10);
            document.add(textoMotivo);
        }
    }

    private void agregarContenidoCartaPresentacion(Document document, Map<String, String> datos, PdfFont font, PdfFont boldFont) throws Exception {
        Paragraph texto = new Paragraph("Me dirijo a usted para presentar formalmente mi solicitud de residencia profesional.")
                .setFont(font)
                .setFontSize(10)
                .setMarginBottom(10);
        document.add(texto);

        if (datos.containsKey("destinatario")) {
            Table tabla = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
            tabla.setWidth(UnitValue.createPercentValue(100));
            tabla.addCell(crearCelda("DIRIGIDO A:", boldFont));
            tabla.addCell(crearCelda(convertirAMayusculasSinAcentos(datos.get("destinatario")), font));
            document.add(tabla);
        }

        if (datos.containsKey("cuerpo")) {
            Paragraph cuerpo = new Paragraph(convertirAMayusculasSinAcentos(datos.get("cuerpo")))
                    .setFont(font)
                    .setFontSize(10)
                    .setMarginTop(10);
            document.add(cuerpo);
        }
    }

    private void agregarContenidoCartaAceptacion(Document document, Map<String, String> datos, PdfFont font, PdfFont boldFont) throws Exception {
        Paragraph texto = new Paragraph("Por medio de la presente, confirmo la aceptación de la residencia profesional.")
                .setFont(font)
                .setFontSize(10)
                .setMarginBottom(10);
        document.add(texto);

        if (datos.containsKey("fechaInicio")) {
            Table tabla = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
            tabla.setWidth(UnitValue.createPercentValue(100));
            tabla.addCell(crearCelda("FECHA DE INICIO:", boldFont));
            tabla.addCell(crearCelda(datos.get("fechaInicio"), font));
            tabla.addCell(crearCelda("FECHA DE TÉRMINO:", boldFont));
            tabla.addCell(crearCelda(datos.getOrDefault("fechaTermino", ""), font));
            document.add(tabla);
        }
    }

    private void agregarContenidoGenerico(Document document, Map<String, String> datos, PdfFont font, PdfFont boldFont) throws Exception {
        // Contenido genérico para documentos sin formato específico
        for (Map.Entry<String, String> entry : datos.entrySet()) {
            if (!entry.getKey().equals("nombreEmpresa") && !entry.getKey().equals("motivo") &&
                    !entry.getKey().equals("destinatario") && !entry.getKey().equals("cuerpo") &&
                    !entry.getKey().equals("fechaInicio") && !entry.getKey().equals("fechaTermino")) {
                Table tabla = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
                tabla.setWidth(UnitValue.createPercentValue(100));
                tabla.addCell(crearCelda(entry.getKey().toUpperCase() + ":", boldFont));
                tabla.addCell(crearCelda(convertirAMayusculasSinAcentos(entry.getValue()), font));
                document.add(tabla);
            }
        }
    }

    private void agregarPiePagina(Document document, PdfFont font) throws Exception {
        String fechaGeneracion = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        Paragraph pie = new Paragraph("DOCUMENTO GENERADO EL: " + fechaGeneracion)
                .setFont(font)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(20);
        document.add(pie);
    }

    private Cell crearCelda(String texto, PdfFont font) throws Exception {
        return new Cell().add(new Paragraph(texto).setFont(font).setFontSize(9))
                .setPadding(3);
    }

    private String convertirAMayusculasSinAcentos(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        String textoMayus = texto.toUpperCase();
        textoMayus = textoMayus.replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace("Ñ", "N")
                .replace("Ü", "U");
        return textoMayus.trim();
    }
}

