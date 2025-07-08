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
    private static final String LUGAR = "CHILPANCINGO DE LOS BRAVO, GUERRERO";
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
        Paragraph titulo = new Paragraph("INSTITUTO TECNOLOGICO DE CHILPANCINGO")
                .setFont(boldFont)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(titulo);

        Paragraph division = new Paragraph("DIVISION DE ESTUDIOS PROFESIONALES\nRESIDENCIA PROFESIONAL\nSOLICITUD DE RESIDENCIA PROFESIONAL")
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
            carreraAlumno = convertirAMayusculasSinAcentos(alumno.optString("carrera", ""));
            coordinador = obtenerCoordinadorPorCarrera(carreraAlumno);
        }

        // Tabla de información básica con datos automáticos
        Table tablaBasica = new Table(UnitValue.createPercentArray(new float[]{15, 35, 15, 35}));
        tablaBasica.setWidth(UnitValue.createPercentValue(100));

        // Fecha actual del sistema
        String fechaActual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        tablaBasica.addCell(crearCelda("LUGAR:", boldFont));
        tablaBasica.addCell(crearCelda(LUGAR, font));

        tablaBasica.addCell(crearCelda("FECHA:", boldFont));
        tablaBasica.addCell(crearCelda(fechaActual, font));

        tablaBasica.addCell(crearCelda("C:", boldFont));
        tablaBasica.addCell(crearCelda(JEFE_DIVISION, font));

        tablaBasica.addCell(crearCelda("ATN. C:", boldFont));
        tablaBasica.addCell(crearCelda(coordinador, font));

        document.add(tablaBasica);
        document.add(new Paragraph("\n"));

        // Información del proyecto
        Paragraph jefe = new Paragraph("JEFE DE LA DIV. DE ESTUDIOS PROFESIONALES     COORD. DE LA CARRERA DE " + carreraAlumno)
                .setFont(boldFont)
                .setFontSize(10)
                .setMarginBottom(10);
        document.add(jefe);

        Table tablaProyecto = new Table(UnitValue.createPercentArray(new float[]{25, 75}));
        tablaProyecto.setWidth(UnitValue.createPercentValue(100));

        tablaProyecto.addCell(crearCelda("NOMBRE DEL PROYECTO:", boldFont));
        tablaProyecto.addCell(crearCelda(convertirAMayusculasSinAcentos(protocolo.optString("nombreProyecto", "")), font));

        tablaProyecto.addCell(crearCelda("OPCION ELEGIDA:", boldFont));
        tablaProyecto.addCell(crearCelda("BANCO DE PROYECTOS: " + convertirAMayusculasSinAcentos(protocolo.optString("banco", "")), font));

        document.add(tablaProyecto);
        document.add(new Paragraph("\n"));
    }

    // Método para obtener coordinador según la carrera
    private String obtenerCoordinadorPorCarrera(String carrera) {
        if (carrera == null || carrera.isEmpty()) {
            return "COORDINADOR DE CARRERA";
        }

        String carreraLower = carrera.toLowerCase();

        if (carreraLower.contains("contabilidad") || carreraLower.contains("gestion empresarial")) {
            return "LIC. MARIA ALCOCER SOLACHE";
        } else if (carreraLower.contains("civil")) {
            return "ING. ALBERTO CARBAJAL RAMIREZ";
        } else if (carreraLower.contains("sistemas") || carreraLower.contains("informatica") ||
                carreraLower.contains("computacionales")) {
            return "SANDRA ALICIA KEMECHS DEL RIO";
        } else {
            return "COORDINADOR DE CARRERA";
        }
    }

    private void agregarSeccionAlumno(Document document, JSONObject protocolo, PdfFont font, PdfFont boldFont) throws Exception {
        // Obtener datos del alumno
        JSONObject alumno = fileManager.buscarAlumnoPorId(protocolo.optString("alumnoId", ""));

        Paragraph datosResidente = new Paragraph("DATOS DEL RESIDENTE:")
                .setFont(boldFont)
                .setFontSize(12)
                .setMarginBottom(10);
        document.add(datosResidente);

        Table tablaAlumno = new Table(UnitValue.createPercentArray(new float[]{20, 40, 20, 20}));
        tablaAlumno.setWidth(UnitValue.createPercentValue(100));

        if (alumno != null) {
            tablaAlumno.addCell(crearCelda("NOMBRE:", boldFont));
            tablaAlumno.addCell(crearCelda(convertirAMayusculasSinAcentos(alumno.optString("nombre", "")), font));
            tablaAlumno.addCell(crearCelda("SEXO:", boldFont));
            tablaAlumno.addCell(crearCelda(convertirAMayusculasSinAcentos(alumno.optString("sexo", "")), font));

            tablaAlumno.addCell(crearCelda("CARRERA:", boldFont));
            tablaAlumno.addCell(crearCelda(convertirAMayusculasSinAcentos(alumno.optString("carrera", "")), font));
            tablaAlumno.addCell(crearCelda("NO. DE CONTROL:", boldFont));
            tablaAlumno.addCell(crearCelda(convertirAMayusculasSinAcentos(alumno.optString("numControl", "")), font));

            tablaAlumno.addCell(crearCelda("DOMICILIO:", boldFont));
            tablaAlumno.addCell(crearCelda(convertirAMayusculasSinAcentos(alumno.optString("direccion", "")), font));
            tablaAlumno.addCell(crearCelda("E-MAIL:", boldFont));
            tablaAlumno.addCell(crearCelda(alumno.optString("email", ""), font)); // Email se mantiene original

            tablaAlumno.addCell(crearCelda("CIUDAD:", boldFont));
            tablaAlumno.addCell(crearCelda(convertirAMayusculasSinAcentos(protocolo.optString("ciudad", "")), font));
            tablaAlumno.addCell(crearCelda("TELEFONO:", boldFont));
            tablaAlumno.addCell(crearCelda(alumno.optString("telefono", ""), font));
        }

        document.add(tablaAlumno);
        document.add(new Paragraph("\n"));
    }

    private void agregarSeccionEmpresa(Document document, JSONObject protocolo, PdfFont font, PdfFont boldFont) throws Exception {
        Paragraph datosEmpresa = new Paragraph("DATOS DE LA INSTITUCION O EMPRESA DONDE REALIZARA LA RESIDENCIA PROFESIONAL:")
                .setFont(boldFont)
                .setFontSize(12)
                .setMarginBottom(10);
        document.add(datosEmpresa);

        // Tabla principal de empresa - SIN FAX
        Table tablaEmpresa = new Table(UnitValue.createPercentArray(new float[]{20, 30, 20, 30}));
        tablaEmpresa.setWidth(UnitValue.createPercentValue(100));

        tablaEmpresa.addCell(crearCelda("NOMBRE:", boldFont));
        tablaEmpresa.addCell(crearCelda(convertirAMayusculasSinAcentos(protocolo.optString("nombreEmpresa", "")), font));
        tablaEmpresa.addCell(crearCelda("R.F.C:", boldFont));
        tablaEmpresa.addCell(crearCelda(protocolo.optString("rfc", "").toUpperCase(), font));

        String giroCompleto = convertirAMayusculasSinAcentos(protocolo.optString("giro", ""));
        tablaEmpresa.addCell(crearCelda("GIRO, RAMO O SECTOR:", boldFont));
        tablaEmpresa.addCell(crearCelda(giroCompleto, font));
        tablaEmpresa.addCell(crearCelda("", font));
        tablaEmpresa.addCell(crearCelda("", font));

        tablaEmpresa.addCell(crearCelda("DOMICILIO:", boldFont));
        tablaEmpresa.addCell(crearCelda(convertirAMayusculasSinAcentos(protocolo.optString("domicilio", "")), font));
        tablaEmpresa.addCell(crearCelda("COLONIA:", boldFont));
        tablaEmpresa.addCell(crearCelda(convertirAMayusculasSinAcentos(protocolo.optString("colonia", "")), font));

        tablaEmpresa.addCell(crearCelda("CIUDAD:", boldFont));
        tablaEmpresa.addCell(crearCelda(convertirAMayusculasSinAcentos(protocolo.optString("ciudad", "")), font));
        tablaEmpresa.addCell(crearCelda("C.P:", boldFont));
        tablaEmpresa.addCell(crearCelda(protocolo.optString("codigoPostal", ""), font));

        // Solo teléfono, sin fax
        tablaEmpresa.addCell(crearCelda("TELEFONO:", boldFont));
        tablaEmpresa.addCell(crearCelda(protocolo.optString("celular", ""), font));
        tablaEmpresa.addCell(crearCelda("", font));
        tablaEmpresa.addCell(crearCelda("", font));

        document.add(tablaEmpresa);
        document.add(new Paragraph("\n"));

        // Tabla separada para la misión
        Table tablaMision = new Table(UnitValue.createPercentArray(new float[]{25, 75}));
        tablaMision.setWidth(UnitValue.createPercentValue(100));

        tablaMision.addCell(crearCelda("MISION DE LA EMPRESA:", boldFont));
        tablaMision.addCell(crearCelda(convertirAMayusculasSinAcentos(protocolo.optString("mision", "")), font));

        document.add(tablaMision);
        document.add(new Paragraph("\n"));

        // Información de contactos CON PUESTOS
        Table tablaContactos = new Table(UnitValue.createPercentArray(new float[]{30, 35, 35}));
        tablaContactos.setWidth(UnitValue.createPercentValue(100));

        tablaContactos.addCell(crearCelda("NOMBRE DEL TITULAR:", boldFont));
        tablaContactos.addCell(crearCelda(convertirAMayusculasSinAcentos(protocolo.optString("titular", "")), font));
        tablaContactos.addCell(crearCelda("PUESTO: " + convertirAMayusculasSinAcentos(protocolo.optString("puestoTitular", "")), font));

        tablaContactos.addCell(crearCelda("NOMBRE DEL ASESOR EXTERNO:", boldFont));
        tablaContactos.addCell(crearCelda(convertirAMayusculasSinAcentos(protocolo.optString("asesorExterno", "")), font));
        tablaContactos.addCell(crearCelda("PUESTO: " + convertirAMayusculasSinAcentos(protocolo.optString("puestoAsesor", "")), font));

        tablaContactos.addCell(crearCelda("NOMBRE DE LA PERSONA QUE FIRMARA EL CONVENIO:", boldFont));
        tablaContactos.addCell(crearCelda(convertirAMayusculasSinAcentos(protocolo.optString("firmante", "")), font));
        tablaContactos.addCell(crearCelda("PUESTO: " + convertirAMayusculasSinAcentos(protocolo.optString("puestoFirmante", "")), font));

        document.add(tablaContactos);

        // Agregar fecha de generación al final
        document.add(new Paragraph("\n"));
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

    // ⬅️ NUEVO MÉTODO: CONVIERTE A MAYÚSCULAS Y QUITA ACENTOS
    private String convertirAMayusculasSinAcentos(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }

        // Convertir a mayúsculas
        String textoMayus = texto.toUpperCase();

        // Quitar acentos y caracteres especiales
        textoMayus = textoMayus.replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace("Ñ", "N")
                .replace("Ü", "U")
                .replace("À", "A")
                .replace("È", "E")
                .replace("Ì", "I")
                .replace("Ò", "O")
                .replace("Ù", "U")
                // Remover caracteres problemáticos
                .replace("Ã¡", "A")
                .replace("Ã©", "E")
                .replace("Ã­", "I")
                .replace("Ã³", "O")
                .replace("Ãº", "U")
                .replace("Ã±", "N")
                .replace("Ã¼", "U")
                .replace("Â", "")
                .replace("\u00A0", " ")
                .replace("\u2018", "'")
                .replace("\u2019", "'")
                .replace("\u201C", "\"")
                .replace("\u201D", "\"")
                .replace("\u2013", "-")
                .replace("\u2014", "-");

        return textoMayus.trim();
    }
}