package com.example.sharedpreferencesapp;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.itextpdf.io.font.constants.StandardFonts;
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

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PDFGeneratorExterno {
    private static final String TAG = "PDFGeneratorExterno";
    private Context context;
    private FileManager fileManager;

    // Datos institucionales predefinidos
    private static final String LUGAR = "Chilpancingo de los Bravo, Guerrero";
    private static final String JEFE_DIVISION = "M.A. MOISES VAZQUEZ PENA";

    public PDFGeneratorExterno(Context context) {
        this.context = context;
        this.fileManager = new FileManager(context);
    }

    public boolean generarPDFProtocoloEnUri(JSONObject protocolo, Uri uri) {
        try {
            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
            if (outputStream == null) {
                Log.e(TAG, "No se pudo abrir el OutputStream");
                return false;
            }

            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            try {
                PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA, "UTF-8");
                PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD, "UTF-8");

                // Encabezado institucional
                agregarEncabezado(document, boldFont);

                // Información del proyecto con datos automáticos
                agregarSeccionProyecto(document, protocolo, font, boldFont);

                // Información del alumno
                agregarSeccionAlumno(document, protocolo, font, boldFont);

                // Información de la empresa
                agregarSeccionEmpresa(document, protocolo, font, boldFont);

            } finally {
                document.close();
                outputStream.close();
            }

            Log.d(TAG, "PDF generado exitosamente en: " + uri.toString());
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error generando PDF", e);
            return false;
        }
    }

    private void agregarEncabezado(Document document, PdfFont boldFont) throws Exception {
        Paragraph titulo = new Paragraph("INSTITUTO TECNOLÓGICO DE CHILPANCINGO")
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
        // Obtener carrera del alumno para determinar coordinador
        JSONObject alumno = fileManager.buscarAlumnoPorId(protocolo.optString("alumnoId", ""));
        String carreraAlumno = "";
        String coordinador = "";

        if (alumno != null) {
            carreraAlumno = limpiarTexto(alumno.optString("carrera", ""));
            coordinador = obtenerCoordinadorPorCarrera(carreraAlumno);
        }

        // Tabla de información básica con datos automáticos
        Table tablaBasica = new Table(UnitValue.createPercentArray(new float[]{15, 35, 15, 35}));
        tablaBasica.setWidth(UnitValue.createPercentValue(100));

        // Fecha actual del sistema
        String fechaActual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        tablaBasica.addCell(crearCelda("Lugar:", boldFont));
        tablaBasica.addCell(crearCelda(LUGAR, font));

        tablaBasica.addCell(crearCelda("Fecha:", boldFont));
        tablaBasica.addCell(crearCelda(fechaActual, font));

        tablaBasica.addCell(crearCelda("C:", boldFont));
        tablaBasica.addCell(crearCelda(JEFE_DIVISION, font));

        tablaBasica.addCell(crearCelda("ATN. C:", boldFont));
        tablaBasica.addCell(crearCelda(coordinador, font));

        document.add(tablaBasica);
        document.add(new Paragraph("\n"));

        // Información del proyecto
        Paragraph jefe = new Paragraph("Jefe de la Div. de Estudios Profesionales     Coord. de la Carrera de " + carreraAlumno)
                .setFont(boldFont)
                .setFontSize(10)
                .setMarginBottom(10);
        document.add(jefe);

        Table tablaProyecto = new Table(UnitValue.createPercentArray(new float[]{25, 75}));
        tablaProyecto.setWidth(UnitValue.createPercentValue(100));

        tablaProyecto.addCell(crearCelda("NOMBRE DEL PROYECTO:", boldFont));
        tablaProyecto.addCell(crearCelda(limpiarTexto(protocolo.optString("nombreProyecto", "")), font));

        tablaProyecto.addCell(crearCelda("OPCIÓN ELEGIDA:", boldFont));
        tablaProyecto.addCell(crearCelda("Banco de Proyectos: " + limpiarTexto(protocolo.optString("banco", "")), font));

        document.add(tablaProyecto);
        document.add(new Paragraph("\n"));
    }

    // Método para obtener coordinador según la carrera
    private String obtenerCoordinadorPorCarrera(String carrera) {
        if (carrera == null || carrera.isEmpty()) {
            return "COORDINADOR DE CARRERA";
        }

        String carreraLower = carrera.toLowerCase();

        if (carreraLower.contains("contabilidad") || carreraLower.contains("gestión empresarial") ||
                carreraLower.contains("gestion empresarial")) {
            return "LIC. MARIA ALCOCER SOLACHE";
        } else if (carreraLower.contains("civil")) {
            return "ING. ALBERTO CARBAJAL RAMIREZ";
        } else if (carreraLower.contains("sistemas") || carreraLower.contains("informática") ||
                carreraLower.contains("informatica") || carreraLower.contains("computacionales")) {
            return "SANDRA ALICIA KEMECHS DEL RIO";
        } else {
            return "COORDINADOR DE CARRERA";
        }
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
            tablaAlumno.addCell(crearCelda(limpiarTexto(alumno.optString("nombre", "")), font));
            tablaAlumno.addCell(crearCelda("Sexo:", boldFont));
            tablaAlumno.addCell(crearCelda(limpiarTexto(alumno.optString("sexo", "")), font));

            tablaAlumno.addCell(crearCelda("Carrera:", boldFont));
            tablaAlumno.addCell(crearCelda(limpiarTexto(alumno.optString("carrera", "")), font));
            tablaAlumno.addCell(crearCelda("No. de control:", boldFont));
            tablaAlumno.addCell(crearCelda(limpiarTexto(alumno.optString("numControl", "")), font));

            tablaAlumno.addCell(crearCelda("Domicilio:", boldFont));
            tablaAlumno.addCell(crearCelda(limpiarTexto(alumno.optString("direccion", "")), font));
            tablaAlumno.addCell(crearCelda("E-mail:", boldFont));
            tablaAlumno.addCell(crearCelda(limpiarTexto(alumno.optString("email", "")), font));

            tablaAlumno.addCell(crearCelda("Ciudad:", boldFont));
            tablaAlumno.addCell(crearCelda(limpiarTexto(protocolo.optString("ciudad", "")), font));
            tablaAlumno.addCell(crearCelda("Teléfono:", boldFont));
            tablaAlumno.addCell(crearCelda(limpiarTexto(alumno.optString("telefono", "")), font));
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

        // Tabla principal de empresa - SIN FAX
        Table tablaEmpresa = new Table(UnitValue.createPercentArray(new float[]{20, 30, 20, 30}));
        tablaEmpresa.setWidth(UnitValue.createPercentValue(100));

        tablaEmpresa.addCell(crearCelda("Nombre:", boldFont));
        tablaEmpresa.addCell(crearCelda(limpiarTexto(protocolo.optString("nombreEmpresa", "")), font));
        tablaEmpresa.addCell(crearCelda("R.F.C:", boldFont));
        tablaEmpresa.addCell(crearCelda(limpiarTexto(protocolo.optString("rfc", "")), font));

        String giroCompleto = limpiarTexto(protocolo.optString("giro", ""));
        tablaEmpresa.addCell(crearCelda("Giro, Ramo o Sector:", boldFont));
        tablaEmpresa.addCell(crearCelda(giroCompleto, font));
        tablaEmpresa.addCell(crearCelda("", font));
        tablaEmpresa.addCell(crearCelda("", font));

        tablaEmpresa.addCell(crearCelda("Domicilio:", boldFont));
        tablaEmpresa.addCell(crearCelda(limpiarTexto(protocolo.optString("domicilio", "")), font));
        tablaEmpresa.addCell(crearCelda("Colonia:", boldFont));
        tablaEmpresa.addCell(crearCelda(limpiarTexto(protocolo.optString("colonia", "")), font));

        tablaEmpresa.addCell(crearCelda("Ciudad:", boldFont));
        tablaEmpresa.addCell(crearCelda(limpiarTexto(protocolo.optString("ciudad", "")), font));
        tablaEmpresa.addCell(crearCelda("C.P:", boldFont));
        tablaEmpresa.addCell(crearCelda(limpiarTexto(protocolo.optString("codigoPostal", "")), font));

        // Solo teléfono, sin fax
        tablaEmpresa.addCell(crearCelda("Teléfono:", boldFont));
        tablaEmpresa.addCell(crearCelda(limpiarTexto(protocolo.optString("celular", "")), font));
        tablaEmpresa.addCell(crearCelda("", font));
        tablaEmpresa.addCell(crearCelda("", font));

        document.add(tablaEmpresa);
        document.add(new Paragraph("\n"));

        // Tabla separada para la misión
        Table tablaMision = new Table(UnitValue.createPercentArray(new float[]{25, 75}));
        tablaMision.setWidth(UnitValue.createPercentValue(100));

        tablaMision.addCell(crearCelda("Misión de la Empresa:", boldFont));
        tablaMision.addCell(crearCelda(limpiarTexto(protocolo.optString("mision", "")), font));

        document.add(tablaMision);
        document.add(new Paragraph("\n"));

        // Información de contactos CON PUESTOS
        Table tablaContactos = new Table(UnitValue.createPercentArray(new float[]{30, 35, 35}));
        tablaContactos.setWidth(UnitValue.createPercentValue(100));

        tablaContactos.addCell(crearCelda("Nombre del Titular:", boldFont));
        tablaContactos.addCell(crearCelda(limpiarTexto(protocolo.optString("titular", "")), font));
        tablaContactos.addCell(crearCelda("Puesto: " + limpiarTexto(protocolo.optString("puestoTitular", "")), font));

        tablaContactos.addCell(crearCelda("Nombre del Asesor Externo:", boldFont));
        tablaContactos.addCell(crearCelda(limpiarTexto(protocolo.optString("asesorExterno", "")), font));
        tablaContactos.addCell(crearCelda("Puesto: " + limpiarTexto(protocolo.optString("puestoAsesor", "")), font));

        tablaContactos.addCell(crearCelda("Nombre de la persona que firmará el Convenio:", boldFont));
        tablaContactos.addCell(crearCelda(limpiarTexto(protocolo.optString("firmante", "")), font));
        tablaContactos.addCell(crearCelda("Puesto: " + limpiarTexto(protocolo.optString("puestoFirmante", "")), font));

        document.add(tablaContactos);

        // Agregar fecha de generación al final
        document.add(new Paragraph("\n"));
        String fechaGeneracion = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        Paragraph pie = new Paragraph("Documento generado el: " + fechaGeneracion)
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

    // Método mejorado para limpiar texto con acentos
    private String limpiarTexto(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }

        try {
            // Asegurar que el texto esté en UTF-8
            byte[] bytes = texto.getBytes("UTF-8");
            String textoLimpio = new String(bytes, "UTF-8");

            // Reemplazar caracteres problemáticos usando códigos Unicode
            textoLimpio = textoLimpio.replace("\u2018", "'")  // Comilla simple izquierda
                    .replace("\u2019", "'")    // Comilla simple derecha
                    .replace("\u201C", "\"")   // Comilla doble izquierda
                    .replace("\u201D", "\"")   // Comilla doble derecha
                    .replace("\u2013", "-")    // En dash
                    .replace("\u2014", "-")    // Em dash
                    .replace("\u00A0", " ")    // Espacio no rompible
                    .replace("\u00C3\u00A1", "á")    // á mal codificada
                    .replace("\u00C3\u00A9", "é")    // é mal codificada
                    .replace("\u00C3\u00AD", "í")    // í mal codificada
                    .replace("\u00C3\u00B3", "ó")    // ó mal codificada
                    .replace("\u00C3\u00BA", "ú")    // ú mal codificada
                    .replace("\u00C3\u00B1", "ñ")    // ñ mal codificada
                    .replace("\u00C3\u00BC", "ü");   // ü mal codificada

            return textoLimpio;
        } catch (Exception e) {
            Log.w(TAG, "Error procesando texto: " + texto, e);
            return texto; // Devolver original si hay error
        }
    }
}