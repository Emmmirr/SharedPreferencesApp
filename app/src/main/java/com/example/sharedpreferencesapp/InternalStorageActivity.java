package com.example.sharedpreferencesapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class InternalStorageActivity extends AppCompatActivity {

    private static final String ARCHIVO_NOMBRE = "datos_internos.txt";
    
    private EditText etTexto;
    private Button btnGuardar, btnLeer, btnEliminar;
    private TextView tvEstado, tvContenido;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_internal_storage);

        // Inicializar vistas
        initViews();
        
        // Configurar listeners
        setupListeners();
        
        // Mensaje inicial
        tvEstado.setText("Seleccione una acción para comenzar");
    }

    private void initViews() {
        etTexto = findViewById(R.id.etTexto);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnLeer = findViewById(R.id.btnLeer);
        btnEliminar = findViewById(R.id.btnEliminar);
        tvEstado = findViewById(R.id.tvEstado);
        tvContenido = findViewById(R.id.tvContenido);
    }

    private void setupListeners() {
        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                escribirArchivo();
            }
        });

        btnLeer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                leerArchivo();
            }
        });

        btnEliminar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eliminarArchivo();
            }
        });
    }

    /**
     * Método para escribir datos en un archivo en memoria interna
     */
    private void escribirArchivo() {
        String texto = etTexto.getText().toString().trim();
        
        if (texto.isEmpty()) {
            tvEstado.setText("Error: Debe ingresar texto para guardar");
            tvEstado.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            Toast.makeText(this, "Por favor ingrese texto para guardar", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Usar openFileOutput() para escribir en memoria interna
            FileOutputStream fos = openFileOutput(ARCHIVO_NOMBRE, MODE_PRIVATE);
            fos.write(texto.getBytes());
            fos.close();
            
            // Mostrar mensaje de éxito
            tvEstado.setText("✓ Archivo guardado exitosamente en memoria interna");
            tvEstado.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            Toast.makeText(this, "Archivo guardado correctamente", Toast.LENGTH_SHORT).show();
            
            // Limpiar el campo de texto después de guardar
            etTexto.setText("");
            
        } catch (IOException e) {
            // Manejo de errores
            String mensajeError = "Error al guardar archivo: " + e.getMessage();
            tvEstado.setText(mensajeError);
            tvEstado.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            Toast.makeText(this, "Error al guardar archivo", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Método para leer datos desde el archivo en memoria interna
     */
    private void leerArchivo() {
        try {
            // Verificar si el archivo existe
            File archivo = new File(getFilesDir(), ARCHIVO_NOMBRE);
            if (!archivo.exists()) {
                tvEstado.setText("El archivo no existe. Guarde contenido primero.");
                tvEstado.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                tvContenido.setVisibility(View.GONE);
                Toast.makeText(this, "No hay archivo para leer", Toast.LENGTH_SHORT).show();
                return;
            }

            // Usar openFileInput() para leer desde memoria interna
            FileInputStream fis = openFileInput(ARCHIVO_NOMBRE);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            
            StringBuilder sb = new StringBuilder();
            String line;
            
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            
            bufferedReader.close();
            
            String contenido = sb.toString();
            
            if (contenido.isEmpty()) {
                tvEstado.setText("El archivo está vacío");
                tvEstado.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                tvContenido.setVisibility(View.GONE);
            } else {
                // Mostrar mensaje de éxito y contenido
                tvEstado.setText("✓ Archivo leído exitosamente");
                tvEstado.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                tvContenido.setText(contenido);
                tvContenido.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Archivo leído correctamente", Toast.LENGTH_SHORT).show();
            }
            
        } catch (IOException e) {
            // Manejo de errores
            String mensajeError = "Error al leer archivo: " + e.getMessage();
            tvEstado.setText(mensajeError);
            tvEstado.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            tvContenido.setVisibility(View.GONE);
            Toast.makeText(this, "Error al leer archivo", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Método para eliminar el archivo de memoria interna
     */
    private void eliminarArchivo() {
        try {
            // Verificar si el archivo existe
            File archivo = new File(getFilesDir(), ARCHIVO_NOMBRE);
            if (!archivo.exists()) {
                tvEstado.setText("No hay archivo para eliminar");
                tvEstado.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                Toast.makeText(this, "No hay archivo para eliminar", Toast.LENGTH_SHORT).show();
                return;
            }

            // Eliminar el archivo
            boolean eliminado = deleteFile(ARCHIVO_NOMBRE);
            
            if (eliminado) {
                // Mostrar mensaje de éxito
                tvEstado.setText("✓ Archivo eliminado exitosamente");
                tvEstado.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                Toast.makeText(this, "Archivo eliminado correctamente", Toast.LENGTH_SHORT).show();
                
                // Limpiar campos y ocultar contenido
                etTexto.setText("");
                tvContenido.setText("");
                tvContenido.setVisibility(View.GONE);
            } else {
                tvEstado.setText("Error: No se pudo eliminar el archivo");
                tvEstado.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                Toast.makeText(this, "Error al eliminar archivo", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            // Manejo de errores
            String mensajeError = "Error al eliminar archivo: " + e.getMessage();
            tvEstado.setText(mensajeError);
            tvEstado.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            Toast.makeText(this, "Error al eliminar archivo", Toast.LENGTH_SHORT).show();
        }
    }
}