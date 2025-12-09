package com.example.sharedpreferencesapp;

import android.util.Log;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WordDocumentFiller {
    private static final String TAG = "WordDocumentFiller";

    /**
     * Llena un documento Word con los datos proporcionados
     * @param inputStream Stream del documento Word original
     * @param outputStream Stream donde se guardará el documento llenado
     * @param datos Mapa con los datos a reemplazar (clave: nombre del marcador sin {{}}, valor: texto a insertar)
     * @return true si fue exitoso, false en caso contrario
     */
    public boolean llenarDocumentoWord(InputStream inputStream, OutputStream outputStream, Map<String, String> datos) {
        try {
            // Cargar el documento Word
            XWPFDocument document = new XWPFDocument(inputStream);

            // Agregar datos automáticos si no están en el mapa
            Calendar calendar = Calendar.getInstance();
            if (!datos.containsKey("dia")) {
                datos.put("dia", String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
            }
            if (!datos.containsKey("mes")) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM", new Locale("es", "ES"));
                datos.put("mes", sdf.format(new Date()));
            }
            if (!datos.containsKey("ano")) {
                datos.put("ano", String.valueOf(calendar.get(Calendar.YEAR)));
            }

            // Reemplazar marcadores en todos los párrafos
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                reemplazarMarcadoresEnParrafo(paragraph, datos);
            }

            // Reemplazar en tablas también
            document.getTables().forEach(table -> {
                table.getRows().forEach(row -> {
                    row.getTableCells().forEach(cell -> {
                        cell.getParagraphs().forEach(paragraph -> {
                            reemplazarMarcadoresEnParrafo(paragraph, datos);
                        });
                    });
                });
            });

            // Guardar el documento
            document.write(outputStream);
            document.close();

            Log.d(TAG, "Documento Word llenado exitosamente");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error llenando documento Word", e);
            return false;
        }
    }

    /**
     * Reemplaza los marcadores {{campo}} en un párrafo con los valores del mapa
     */
    private void reemplazarMarcadoresEnParrafo(XWPFParagraph paragraph, Map<String, String> datos) {
        String textoCompleto = paragraph.getText();
        if (textoCompleto == null || textoCompleto.isEmpty()) {
            return;
        }

        // Reemplazar todos los marcadores en el texto
        String textoReemplazado = textoCompleto;
        for (Map.Entry<String, String> entry : datos.entrySet()) {
            String marcador = "{{" + entry.getKey() + "}}";
            String marcadorConEspacios = "{{" + entry.getKey() + " }}"; // Con espacio antes del cierre
            String valor = entry.getValue() != null ? entry.getValue() : "";

            textoReemplazado = textoReemplazado.replace(marcador, valor)
                    .replace(marcadorConEspacios, valor);
        }

        // Si hubo cambios, reemplazar el contenido del párrafo
        if (!textoReemplazado.equals(textoCompleto)) {
            // Limpiar todos los runs existentes
            for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
                paragraph.removeRun(i);
            }

            // Crear un nuevo run con el texto reemplazado
            XWPFRun run = paragraph.createRun();
            run.setText(textoReemplazado);
        }
    }

    /**
     * Convierte un documento Word a PDF (requiere biblioteca adicional)
     * Por ahora, retornamos el Word llenado
     */
    public boolean convertirWordAPDF(InputStream wordInputStream, OutputStream pdfOutputStream) {
        // TODO: Implementar conversión Word a PDF si es necesario
        // Por ahora, el Word llenado se puede usar directamente
        // O se puede usar una biblioteca como docx4j o iText para convertir
        return false;
    }
}

