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
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BloquesAutorizadosFragment extends Fragment {

    private static final String TAG = "BloquesAutorizadosFragment";
    private RecyclerView recyclerBloques;
    private ProgressBar progressBar;
    private TextView tvNoBloques;
    private MaterialButton btnCrearBloque;
    private ImageView btnBack;
    private FirebaseManager firebaseManager;
    private List<Map<String, Object>> bloquesList = new ArrayList<>();
    private BloqueAutorizadoAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bloques_autorizados, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerBloques = view.findViewById(R.id.recycler_bloques);
        progressBar = view.findViewById(R.id.progress_bar);
        tvNoBloques = view.findViewById(R.id.tv_no_bloques);
        btnCrearBloque = view.findViewById(R.id.btn_crear_bloque);
        btnBack = view.findViewById(R.id.btn_back);

        // Configurar RecyclerView
        recyclerBloques.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BloqueAutorizadoAdapter(getContext(), bloquesList,
                this::abrirDetalleBloque,
                this::eliminarBloque);
        recyclerBloques.setAdapter(adapter);

        // Inicializar Firebase
        firebaseManager = new FirebaseManager();

        // Botón de regreso
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Botón crear bloque
        btnCrearBloque.setOnClickListener(v -> mostrarDialogoCrearBloque());

        // Cargar bloques
        loadBloques();
    }

    private void loadBloques() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoBloques.setVisibility(View.GONE);

        firebaseManager.obtenerBloquesAutorizados(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                bloquesList.clear();
                List<Map<String, Object>> bloquesTemp = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Map<String, Object> bloqueData = new HashMap<>(document.getData());
                    bloqueData.put("id", document.getId());
                    bloquesTemp.add(bloqueData);
                }
                // Ordenar por fechaCreacion descendente manualmente
                bloquesTemp.sort((b1, b2) -> {
                    Long fecha1 = (Long) b1.get("fechaCreacion");
                    Long fecha2 = (Long) b2.get("fechaCreacion");
                    if (fecha1 == null) fecha1 = 0L;
                    if (fecha2 == null) fecha2 = 0L;
                    return fecha2.compareTo(fecha1); // Descendente
                });
                bloquesList.addAll(bloquesTemp);
                adapter.notifyDataSetChanged();
                updateEmptyState();
            } else {
                Log.e(TAG, "Error cargando bloques", task.getException());
                Toast.makeText(getContext(), "Error al cargar bloques", Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void mostrarDialogoCrearBloque() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_crear_bloque, null);
        builder.setView(dialogView);

        TextInputEditText etNombreBloque = dialogView.findViewById(R.id.et_nombre_bloque);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btn_cancelar);
        MaterialButton btnCrear = dialogView.findViewById(R.id.btn_crear);

        AlertDialog dialog = builder.create();

        btnCancelar.setOnClickListener(v -> dialog.dismiss());
        btnCrear.setOnClickListener(v -> {
            String nombreBloque = etNombreBloque.getText().toString().trim();
            if (nombreBloque.isEmpty()) {
                Toast.makeText(getContext(), "Por favor ingresa un nombre para el bloque", Toast.LENGTH_SHORT).show();
                return;
            }

            firebaseManager.crearBloqueAutorizado(nombreBloque,
                    () -> {
                        Toast.makeText(getContext(), "Bloque creado exitosamente", Toast.LENGTH_SHORT).show();
                        loadBloques();
                        dialog.dismiss();
                    },
                    e -> {
                        Log.e(TAG, "Error creando bloque", e);
                        Toast.makeText(getContext(), "Error al crear bloque: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    private void abrirDetalleBloque(String bloqueId, String nombreBloque) {
        DetalleBloqueFragment fragment = new DetalleBloqueFragment();
        Bundle args = new Bundle();
        args.putString("bloqueId", bloqueId);
        args.putString("nombreBloque", nombreBloque);
        fragment.setArguments(args);

        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.nav_host_fragment, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void eliminarBloque(String bloqueId, String nombreBloque) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Bloque")
                .setMessage("¿Estás seguro de que deseas eliminar el bloque \"" + nombreBloque + "\"?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    firebaseManager.eliminarBloque(bloqueId,
                            () -> {
                                Toast.makeText(getContext(), "Bloque eliminado", Toast.LENGTH_SHORT).show();
                                loadBloques();
                            },
                            e -> {
                                Log.e(TAG, "Error eliminando bloque", e);
                                Toast.makeText(getContext(), "Error al eliminar bloque: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateEmptyState() {
        if (bloquesList.isEmpty()) {
            tvNoBloques.setVisibility(View.VISIBLE);
            recyclerBloques.setVisibility(View.GONE);
        } else {
            tvNoBloques.setVisibility(View.GONE);
            recyclerBloques.setVisibility(View.VISIBLE);
        }
    }
}

