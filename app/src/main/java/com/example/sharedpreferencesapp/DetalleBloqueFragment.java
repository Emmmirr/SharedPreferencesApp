package com.example.sharedpreferencesapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DetalleBloqueFragment extends Fragment {

    private static final String TAG = "DetalleBloqueFragment";
    private String bloqueId;
    private String nombreBloque;
    private RecyclerView recyclerNumeros;
    private ProgressBar progressBar;
    private TextView tvNoNumeros;
    private TextView tvNombreBloqueHeader;
    private MaterialButton btnAgregarNumero;
    private ImageView btnBack;
    private FirebaseManager firebaseManager;
    private List<String> numerosList = new ArrayList<>();
    private NumeroAutorizadoAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detalle_bloque, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Obtener argumentos
        Bundle args = getArguments();
        if (args != null) {
            bloqueId = args.getString("bloqueId");
            nombreBloque = args.getString("nombreBloque");
        }

        recyclerNumeros = view.findViewById(R.id.recycler_numeros);
        progressBar = view.findViewById(R.id.progress_bar);
        tvNoNumeros = view.findViewById(R.id.tv_no_numeros);
        tvNombreBloqueHeader = view.findViewById(R.id.tv_nombre_bloque_header);
        btnAgregarNumero = view.findViewById(R.id.btn_agregar_numero);
        btnBack = view.findViewById(R.id.btn_back);

        // Configurar RecyclerView
        recyclerNumeros.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NumeroAutorizadoAdapter(getContext(), numerosList, this::eliminarNumero);
        recyclerNumeros.setAdapter(adapter);

        // Inicializar Firebase
        firebaseManager = new FirebaseManager();

        // Configurar UI
        if (nombreBloque != null) {
            tvNombreBloqueHeader.setText(nombreBloque);
        }

        // Botón de regreso
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Botón agregar número
        btnAgregarNumero.setOnClickListener(v -> mostrarDialogoAgregarNumero());

        // Cargar números del bloque
        if (bloqueId != null) {
            loadNumerosBloque();
        }
    }

    private void loadNumerosBloque() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoNumeros.setVisibility(View.GONE);

        firebaseManager.obtenerBloquePorId(bloqueId, task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                DocumentSnapshot document = task.getResult();
                Map<String, Object> data = document.getData();
                if (data != null) {
                    List<String> numeros = (List<String>) data.getOrDefault("numeros", new ArrayList<>());
                    numerosList.clear();
                    numerosList.addAll(numeros);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                }
            } else {
                Log.e(TAG, "Error cargando bloque");
                Toast.makeText(getContext(), "Error al cargar el bloque", Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void mostrarDialogoAgregarNumero() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_agregar_autorizado, null);
        builder.setView(dialogView);

        TextInputEditText etNumeroControl = dialogView.findViewById(R.id.et_numero_control);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btn_cancelar);
        MaterialButton btnAgregar = dialogView.findViewById(R.id.btn_agregar);

        AlertDialog dialog = builder.create();

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        btnAgregar.setOnClickListener(v -> {
            String numeroControl = etNumeroControl.getText().toString().trim();
            if (numeroControl.isEmpty()) {
                Toast.makeText(getContext(), "Por favor ingresa un número de control", Toast.LENGTH_SHORT).show();
                return;
            }

            if (bloqueId != null) {
                firebaseManager.agregarNumeroABloque(bloqueId, numeroControl,
                        () -> {
                            Toast.makeText(getContext(), "Número de control agregado exitosamente", Toast.LENGTH_SHORT).show();
                            loadNumerosBloque();
                            dialog.dismiss();
                        },
                        e -> {
                            Log.e(TAG, "Error agregando número al bloque", e);
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });

        dialog.show();
    }

    private void eliminarNumero(String numeroControl) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Número de Control")
                .setMessage("¿Estás seguro de que deseas eliminar el número de control " + numeroControl + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    if (bloqueId != null) {
                        firebaseManager.eliminarNumeroDeBloque(bloqueId, numeroControl,
                                () -> {
                                    Toast.makeText(getContext(), "Número de control eliminado", Toast.LENGTH_SHORT).show();
                                    loadNumerosBloque();
                                },
                                e -> {
                                    Log.e(TAG, "Error eliminando número del bloque", e);
                                    Toast.makeText(getContext(), "Error al eliminar número: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateEmptyState() {
        if (numerosList.isEmpty()) {
            tvNoNumeros.setVisibility(View.VISIBLE);
            recyclerNumeros.setVisibility(View.GONE);
        } else {
            tvNoNumeros.setVisibility(View.GONE);
            recyclerNumeros.setVisibility(View.VISIBLE);
        }
    }
}

