package com.example.sharedpreferencesapp;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PDFGenerator {
    private static final String TAG = "PDFGenerator";
    private Context context;
    private FileManager fileManager;

    public PDFGenerator(Context context) {
        this.context = context;
        this.fileManager = new FileManager(context);
    }

    /**
     * Genera PDF con la lista completa de alumnos
     */
    public String generarPDFAlumnos(List<JSONObject> alumnos) {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
            String fileName = "alumnos_" + timestamp + ".pdf";
            
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File pdfFile = new File(downloadsDir, fileName);

            PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            // Configurar fuente
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            // Encabezado
            Paragraph header = new Paragraph("LISTA DE ALUMNOS")
                    .setFont(boldFont)
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(header);

            // Fecha de generación
            Paragraph fecha = new Paragraph("Fecha de generación: " + 
                    new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()))
                    .setFont(font)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginBottom(20);
            document.add(fecha);

            // Crear tabla
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 3, 1, 2, 2}))
                    .setWidth(UnitValue.createPercentValue(100));

            // Encabezados de tabla
            table.addHeaderCell(new Cell().add(new Paragraph("Nombre").setFont(boldFont)));
            table.addHeaderCell(new Cell().add(new Paragraph("No. Control").setFont(boldFont)));
            table.addHeaderCell(new Cell().add(new Paragraph("CURP").setFont(boldFont)));
            table.addHeaderCell(new Cell().add(new Paragraph("Sexo").setFont(boldFont)));
            table.addHeaderCell(new Cell().add(new Paragraph("Carrera").setFont(boldFont)));
            table.addHeaderCell(new Cell().add(new Paragraph("Semestre").setFont(boldFont)));

            // Agregar datos de alumnos
            for (JSONObject alumno : alumnos) {
                table.addCell(new Cell().add(new Paragraph(alumno.optString("nombre", "")).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(alumno.optString("numControl", "")).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(alumno.optString("curp", "")).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(alumno.optString("sexo", "")).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(alumno.optString("carrera", "")).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(alumno.optString("semestre", "")).setFont(font)));
            }

            document.add(table);
            
            // Pie de página
            Paragraph footer = new Paragraph("Total de alumnos: " + alumnos.size())
                    .setFont(boldFont)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20);
            document.add(footer);

            document.close();
            
            Log.d(TAG, "PDF de alumnos generado exitosamente: " + pdfFile.getAbsolutePath());
            return pdfFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "Error generando PDF de alumnos", e);
            return null;
        }
    }

    /**
     * Genera PDF con los datos de un alumno individual
     */
    public String generarPDFAlumnoIndividual(JSONObject alumno) {
        try {
            String nombre = alumno.optString("nombre", "alumno");
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
            String fileName = "alumno_" + nombre.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp + ".pdf";
            
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File pdfFile = new File(downloadsDir, fileName);

            PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            // Configurar fuente
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            // Encabezado
            Paragraph header = new Paragraph("INFORMACIÓN DEL ALUMNO")
                    .setFont(boldFont)
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30);
            document.add(header);

            // Crear tabla con datos del alumno
            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                    .setWidth(UnitValue.createPercentValue(100));

            // Agregar datos
            agregarFilaTabla(table, "Nombre:", alumno.optString("nombre", ""), boldFont, font);
            agregarFilaTabla(table, "CURP:", alumno.optString("curp", ""), boldFont, font);
            agregarFilaTabla(table, "Fecha de Nacimiento:", alumno.optString("fechaNacimiento", ""), boldFont, font);
            agregarFilaTabla(table, "Sexo:", alumno.optString("sexo", ""), boldFont, font);
            agregarFilaTabla(table, "No. Control:", alumno.optString("numControl", ""), boldFont, font);
            agregarFilaTabla(table, "Semestre:", alumno.optString("semestre", ""), boldFont, font);
            agregarFilaTabla(table, "Carrera:", alumno.optString("carrera", ""), boldFont, font);
            agregarFilaTabla(table, "Especialidad:", alumno.optString("especialidad", ""), boldFont, font);
            agregarFilaTabla(table, "Teléfono:", alumno.optString("telefono", ""), boldFont, font);
            agregarFilaTabla(table, "Email:", alumno.optString("email", ""), boldFont, font);
            agregarFilaTabla(table, "Dirección:", alumno.optString("direccion", ""), boldFont, font);

            document.add(table);

            // Fecha de generación
            Paragraph fecha = new Paragraph("Fecha de generación: " + 
                    new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()))
                    .setFont(font)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(30);
            document.add(fecha);

            document.close();
            
            Log.d(TAG, "PDF de alumno individual generado: " + pdfFile.getAbsolutePath());
            return pdfFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "Error generando PDF de alumno individual", e);
            return null;
        }
    }

    /**
     * Genera PDF con la lista completa de protocolos
     */
    public String generarPDFProtocolos(List<JSONObject> protocolos) {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
            String fileName = "protocolos_" + timestamp + ".pdf";
            
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File pdfFile = new File(downloadsDir, fileName);

            PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            // Configurar fuente
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            // Encabezado
            Paragraph header = new Paragraph("LISTA DE PROTOCOLOS")
                    .setFont(boldFont)
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(header);

            // Fecha de generación
            Paragraph fecha = new Paragraph("Fecha de generación: " + 
                    new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()))
                    .setFont(font)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginBottom(20);
            document.add(fecha);

            // Crear tabla
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 3, 2, 2}))
                    .setWidth(UnitValue.createPercentValue(100));

            // Encabezados de tabla
            table.addHeaderCell(new Cell().add(new Paragraph("Proyecto").setFont(boldFont)));
            table.addHeaderCell(new Cell().add(new Paragraph("Alumno").setFont(boldFont)));
            table.addHeaderCell(new Cell().add(new Paragraph("Empresa").setFont(boldFont)));
            table.addHeaderCell(new Cell().add(new Paragraph("Banco").setFont(boldFont)));
            table.addHeaderCell(new Cell().add(new Paragraph("Ciudad").setFont(boldFont)));

            // Agregar datos de protocolos
            for (JSONObject protocolo : protocolos) {
                // Obtener datos del alumno asociado
                String alumnoId = protocolo.optString("alumnoId", "");
                JSONObject alumno = fileManager.buscarAlumnoPorId(alumnoId);
                String nombreAlumno = "Sin alumno";
                if (alumno != null) {
                    nombreAlumno = alumno.optString("nombre", "Sin alumno");
                }

                table.addCell(new Cell().add(new Paragraph(protocolo.optString("nombreProyecto", "")).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(nombreAlumno).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(protocolo.optString("nombreEmpresa", "")).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(protocolo.optString("banco", "")).setFont(font)));
                table.addCell(new Cell().add(new Paragraph(protocolo.optString("ciudad", "")).setFont(font)));
            }

            document.add(table);
            
            // Pie de página
            Paragraph footer = new Paragraph("Total de protocolos: " + protocolos.size())
                    .setFont(boldFont)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20);
            document.add(footer);

            document.close();
            
            Log.d(TAG, "PDF de protocolos generado exitosamente: " + pdfFile.getAbsolutePath());
            return pdfFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "Error generando PDF de protocolos", e);
            return null;
        }
    }

    /**
     * Genera PDF con los datos de un protocolo individual
     */
    public String generarPDFProtocoloIndividual(JSONObject protocolo) {
        try {
            String nombreProyecto = protocolo.optString("nombreProyecto", "protocolo");
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
            String fileName = "protocolo_" + nombreProyecto.replaceAll("[^a-zA-Z0-9]", "_") + "_" + timestamp + ".pdf";
            
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File pdfFile = new File(downloadsDir, fileName);

            PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            // Configurar fuente
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            // Encabezado
            Paragraph header = new Paragraph("INFORMACIÓN DEL PROTOCOLO")
                    .setFont(boldFont)
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30);
            document.add(header);

            // Obtener datos del alumno asociado
            String alumnoId = protocolo.optString("alumnoId", "");
            JSONObject alumno = fileManager.buscarAlumnoPorId(alumnoId);

            // Información del protocolo
            Table protocoloTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                    .setWidth(UnitValue.createPercentValue(100));

            agregarFilaTabla(protocoloTable, "Nombre del Proyecto:", protocolo.optString("nombreProyecto", ""), boldFont, font);
            agregarFilaTabla(protocoloTable, "Banco de Proyectos:", protocolo.optString("banco", ""), boldFont, font);
            agregarFilaTabla(protocoloTable, "Asesor:", protocolo.optString("asesor", ""), boldFont, font);

            document.add(protocoloTable);

            // Información de la empresa
            Paragraph empresaHeader = new Paragraph("INFORMACIÓN DE LA EMPRESA")
                    .setFont(boldFont)
                    .setFontSize(14)
                    .setMarginTop(20)
                    .setMarginBottom(10);
            document.add(empresaHeader);

            Table empresaTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                    .setWidth(UnitValue.createPercentValue(100));

            agregarFilaTabla(empresaTable, "Nombre de la Empresa:", protocolo.optString("nombreEmpresa", ""), boldFont, font);
            agregarFilaTabla(empresaTable, "Giro:", protocolo.optString("giro", ""), boldFont, font);
            agregarFilaTabla(empresaTable, "RFC:", protocolo.optString("rfc", ""), boldFont, font);
            agregarFilaTabla(empresaTable, "Domicilio:", protocolo.optString("domicilio", ""), boldFont, font);
            agregarFilaTabla(empresaTable, "Colonia:", protocolo.optString("colonia", ""), boldFont, font);
            agregarFilaTabla(empresaTable, "Código Postal:", protocolo.optString("codigoPostal", ""), boldFont, font);
            agregarFilaTabla(empresaTable, "Ciudad:", protocolo.optString("ciudad", ""), boldFont, font);
            agregarFilaTabla(empresaTable, "Celular:", protocolo.optString("celular", ""), boldFont, font);
            agregarFilaTabla(empresaTable, "Misión:", protocolo.optString("mision", ""), boldFont, font);
            agregarFilaTabla(empresaTable, "Titular:", protocolo.optString("titular", ""), boldFont, font);
            agregarFilaTabla(empresaTable, "Firmante:", protocolo.optString("firmante", ""), boldFont, font);

            document.add(empresaTable);

            // Información del alumno asociado
            if (alumno != null) {
                Paragraph alumnoHeader = new Paragraph("INFORMACIÓN DEL ALUMNO ASOCIADO")
                        .setFont(boldFont)
                        .setFontSize(14)
                        .setMarginTop(20)
                        .setMarginBottom(10);
                document.add(alumnoHeader);

                Table alumnoTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}))
                        .setWidth(UnitValue.createPercentValue(100));

                agregarFilaTabla(alumnoTable, "Nombre:", alumno.optString("nombre", ""), boldFont, font);
                agregarFilaTabla(alumnoTable, "No. Control:", alumno.optString("numControl", ""), boldFont, font);
                agregarFilaTabla(alumnoTable, "CURP:", alumno.optString("curp", ""), boldFont, font);
                agregarFilaTabla(alumnoTable, "Carrera:", alumno.optString("carrera", ""), boldFont, font);
                agregarFilaTabla(alumnoTable, "Semestre:", alumno.optString("semestre", ""), boldFont, font);

                document.add(alumnoTable);
            }

            // Fecha de generación
            Paragraph fecha = new Paragraph("Fecha de generación: " + 
                    new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()))
                    .setFont(font)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(30);
            document.add(fecha);

            document.close();
            
            Log.d(TAG, "PDF de protocolo individual generado: " + pdfFile.getAbsolutePath());
            return pdfFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "Error generando PDF de protocolo individual", e);
            return null;
        }
    }

    /**
     * Método auxiliar para agregar filas a las tablas
     */
    private void agregarFilaTabla(Table table, String label, String value, PdfFont boldFont, PdfFont font) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(boldFont)));
        table.addCell(new Cell().add(new Paragraph(value).setFont(font)));
    }
}