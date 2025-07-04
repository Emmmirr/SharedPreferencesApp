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

public class PDFGeneratorCalendario {
    private static final String TAG = "PDFGeneratorCalendario";
    private Context context;
    private FileManager fileManager;

    public PDFGeneratorCalendario(Context context) {
        this.context = context;
        this.fileManager = new FileManager(context);
    }

    public boolean generarPDFCalendarioEnUri(JSONObject calendario, Uri uri) {
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
                // ⬅️ USAR FUENTES CON SOPORTE UTF-8
                PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA, "UTF-8");
                PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD, "UTF-8");

                agregarEncabezado(document, calendario, boldFont, font);
                agregarCalendarioFechas(document, calendario, font, boldFont);

            } finally {
                document.close();
                outputStream.close();
            }

            Log.d(TAG, "PDF de calendario generado exitosamente");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error generando PDF de calendario", e);
            return false;
        }
    }

    private void agregarEncabezado(Document document, JSONObject calendario, PdfFont boldFont, PdfFont font) throws Exception {
        // Obtener datos del alumno
        JSONObject alumno = fileManager.buscarAlumnoPorId(calendario.optString("alumnoId", ""));

        Paragraph titulo = new Paragraph("CALENDARIO DE RESIDENCIA PROFESIONAL")
                .setFont(boldFont)
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(titulo);

        Paragraph institucion = new Paragraph("INSTITUTO TECNOLÓGICO DE CHILPANCINGO")
                .setFont(boldFont)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(institucion);

        if (alumno != null) {
            Table tablaAlumno = new Table(UnitValue.createPercentArray(new float[]{25, 75}));
            tablaAlumno.setWidth(UnitValue.createPercentValue(100));

            tablaAlumno.addCell(crearCelda("Alumno:", boldFont));
            tablaAlumno.addCell(crearCelda(limpiarTexto(alumno.optString("nombre", "")), font));

            tablaAlumno.addCell(crearCelda("No. Control:", boldFont));
            tablaAlumno.addCell(crearCelda(limpiarTexto(alumno.optString("numControl", "")), font));

            tablaAlumno.addCell(crearCelda("Carrera:", boldFont));
            tablaAlumno.addCell(crearCelda(limpiarTexto(alumno.optString("carrera", "")), font));

            // ⬅️ AGREGAR DATOS ADICIONALES CON VALIDACIÓN DE ACENTOS
            if (alumno.has("email") && !alumno.optString("email", "").isEmpty()) {
                tablaAlumno.addCell(crearCelda("Email:", boldFont));
                tablaAlumno.addCell(crearCelda(limpiarTexto(alumno.optString("email", "")), font));
            }

            if (alumno.has("telefono") && !alumno.optString("telefono", "").isEmpty()) {
                tablaAlumno.addCell(crearCelda("Teléfono:", boldFont));
                tablaAlumno.addCell(crearCelda(limpiarTexto(alumno.optString("telefono", "")), font));
            }

            document.add(tablaAlumno);
            document.add(new Paragraph("\n"));
        }
    }

    private void agregarCalendarioFechas(Document document, JSONObject calendario, PdfFont font, PdfFont boldFont) throws Exception {
        Paragraph tituloCalendario = new Paragraph("CRONOGRAMA DE ACTIVIDADES")
                .setFont(boldFont)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15);
        document.add(tituloCalendario);

        Table tablaFechas = new Table(UnitValue.createPercentArray(new float[]{8, 52, 25, 15}));
        tablaFechas.setWidth(UnitValue.createPercentValue(100));

        // Encabezados
        tablaFechas.addCell(crearCeldaEncabezado("No.", boldFont));
        tablaFechas.addCell(crearCeldaEncabezado("Actividad", boldFont));
        tablaFechas.addCell(crearCeldaEncabezado("Fecha Programada", boldFont));
        tablaFechas.addCell(crearCeldaEncabezado("Estado", boldFont));

        // Actividades con acentos correctos
        String[] actividades = {
                "Entrega de Anteproyecto",
                "Entrega de Viabilidad",
                "Entrega de Modificación",
                "Entrega de Viabilidad Final",
                "Inicio de Residencia",
                "Primer Seguimiento",
                "Segundo Seguimiento",
                "Entrega Final"
        };

        String[] camposFecha = {
                "fechaAnteproyecto", "fechaViabilidad", "fechaModificacion",
                "fechaViabilidadFinal", "fechaInicioResidencia", "fechaPrimerSeguimiento",
                "fechaSegundoSeguimiento", "fechaEntregaFinal"
        };

        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Date fechaActual = new Date();

        for (int i = 0; i < actividades.length; i++) {
            String fecha = limpiarTexto(calendario.optString(camposFecha[i], ""));
            String estado = "Pendiente";

            if (!fecha.isEmpty()) {
                try {
                    Date fechaProgramada = formatoFecha.parse(fecha);
                    if (fechaProgramada != null) {
                        if (fechaProgramada.before(fechaActual)) {
                            estado = "Vencido";
                        } else if (fechaProgramada.equals(fechaActual)) {
                            estado = "Hoy";
                        } else {
                            estado = "Programado";
                        }
                    }
                } catch (Exception e) {
                    estado = "Programado";
                }
            }

            tablaFechas.addCell(crearCelda(String.valueOf(i + 1), font));
            tablaFechas.addCell(crearCelda(limpiarTexto(actividades[i]), font));
            tablaFechas.addCell(crearCelda(fecha.isEmpty() ? "No programado" : fecha, font));

            // ⬅️ CELDAS DE ESTADO CON COLORES CORRECTOS
            Cell celdaEstado = crearCelda(limpiarTexto(estado), font);
            switch (estado) {
                case "Vencido":
                    celdaEstado.setBackgroundColor(ColorConstants.PINK); // ⬅️ CAMBIO: Usar PINK en lugar de LIGHT_GRAY
                    break;
                case "Hoy":
                    celdaEstado.setBackgroundColor(ColorConstants.YELLOW);
                    break;
                case "Programado":
                    celdaEstado.setBackgroundColor(ColorConstants.GREEN); // ⬅️ CAMBIO: Usar GREEN en lugar de LIGHT_GREEN
                    break;
                default:
                    // Pendiente - sin color
                    break;
            }
            tablaFechas.addCell(celdaEstado);
        }

        document.add(tablaFechas);
        document.add(new Paragraph("\n"));

        // Agregar resumen estadístico
        agregarResumenEstadistico(document, calendario, font, boldFont);

        // Pie de página
        String fechaGeneracion = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        Paragraph pie = new Paragraph("Documento generado el: " + fechaGeneracion)
                .setFont(font)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20);
        document.add(pie);
    }

    private void agregarResumenEstadistico(Document document, JSONObject calendario, PdfFont font, PdfFont boldFont) throws Exception {
        Paragraph tituloResumen = new Paragraph("RESUMEN ESTADÍSTICO")
                .setFont(boldFont)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(tituloResumen);

        String[] camposFecha = {
                "fechaAnteproyecto", "fechaViabilidad", "fechaModificacion",
                "fechaViabilidadFinal", "fechaInicioResidencia", "fechaPrimerSeguimiento",
                "fechaSegundoSeguimiento", "fechaEntregaFinal"
        };

        int totalActividades = camposFecha.length;
        int actividadesProgramadas = 0;
        int actividadesCompletadas = 0;
        int actividadesVencidas = 0;

        SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Date fechaActual = new Date();

        for (String campo : camposFecha) {
            String fecha = calendario.optString(campo, "");
            if (!fecha.isEmpty()) {
                actividadesProgramadas++;
                try {
                    Date fechaProgramada = formatoFecha.parse(fecha);
                    if (fechaProgramada != null && fechaProgramada.before(fechaActual)) {
                        actividadesVencidas++;
                    }
                } catch (Exception e) {
                    // Ignorar errores de parsing
                }
            }
        }

        Table tablaResumen = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
        tablaResumen.setWidth(UnitValue.createPercentValue(60));

        tablaResumen.addCell(crearCelda("Total de actividades:", boldFont));
        tablaResumen.addCell(crearCelda(String.valueOf(totalActividades), font));

        tablaResumen.addCell(crearCelda("Actividades programadas:", boldFont));
        tablaResumen.addCell(crearCelda(String.valueOf(actividadesProgramadas), font));

        tablaResumen.addCell(crearCelda("Actividades pendientes:", boldFont));
        tablaResumen.addCell(crearCelda(String.valueOf(totalActividades - actividadesProgramadas), font));

        tablaResumen.addCell(crearCelda("Actividades vencidas:", boldFont));
        tablaResumen.addCell(crearCelda(String.valueOf(actividadesVencidas), font));

        int porcentajeProgreso = (actividadesProgramadas * 100) / totalActividades;
        tablaResumen.addCell(crearCelda("Progreso general:", boldFont));
        tablaResumen.addCell(crearCelda(porcentajeProgreso + "%", font));

        document.add(tablaResumen);
        document.add(new Paragraph("\n"));
    }

    private Cell crearCelda(String texto, PdfFont font) throws Exception {
        return new Cell().add(new Paragraph(texto).setFont(font).setFontSize(10))
                .setPadding(5);
    }

    private Cell crearCeldaEncabezado(String texto, PdfFont boldFont) throws Exception {
        return new Cell().add(new Paragraph(texto).setFont(boldFont).setFontSize(11))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(5);
    }

    // ⬅️ MÉTODO MEJORADO PARA LIMPIAR TEXTO CON ACENTOS
    private String limpiarTexto(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }

        try {
            // Asegurar codificación UTF-8
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
                    .replace("\u00C0", "À")    // À
                    .replace("\u00C1", "Á")    // Á
                    .replace("\u00C9", "É")    // É
                    .replace("\u00CD", "Í")    // Í
                    .replace("\u00D1", "Ñ")    // Ñ
                    .replace("\u00D3", "Ó")    // Ó
                    .replace("\u00DA", "Ú")    // Ú
                    .replace("\u00DC", "Ü")    // Ü
                    .replace("\u00E0", "à")    // à
                    .replace("\u00E1", "á")    // á
                    .replace("\u00E9", "é")    // é
                    .replace("\u00ED", "í")    // í
                    .replace("\u00F1", "ñ")    // ñ
                    .replace("\u00F3", "ó")    // ó
                    .replace("\u00FA", "ú")    // ú
                    .replace("\u00FC", "ü");   // ü

            return textoLimpio;
        } catch (Exception e) {
            Log.w(TAG, "Error procesando texto: " + texto, e);
            return texto; // Devolver original si hay error
        }
    }
}