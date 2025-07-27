package com.example.sharedpreferencesapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentosEstudianteFragment extends Fragment {

    private static final int REQUEST_CODE_PICK_DOCUMENT = 123;

    private RecyclerView recyclerDocumentos;
    private TextView tvSinDocumentos;
    private Button btnSubirDocumento;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private DocumentosAdapter documentosAdapter;
    private List<Documento> documentosList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_documentos_estudiante, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar vistas
        recyclerDocumentos = view.findViewById(R.id.recyclerDocumentos);
        tvSinDocumentos = view.findViewById(R.id.tvSinDocumentos);
        btnSubirDocumento = view.findViewById(R.id.btnSubirDocumento);

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Configurar RecyclerView
        documentosList = new ArrayList<>();
        documentosAdapter = new DocumentosAdapter(documentosList, documento -> {
            // Abrir el documento seleccionado
            abrirDocumento(documento);
        });
        recyclerDocumentos.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerDocumentos.setAdapter(documentosAdapter);

        // Configurar botón para subir documento
        btnSubirDocumento.setOnClickListener(v -> seleccionarDocumento());

        // Cargar documentos del estudiante
        cargarDocumentos();
    }

    private void seleccionarDocumento() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        startActivityForResult(intent, REQUEST_CODE_PICK_DOCUMENT);
    }

    private void cargarDocumentos() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();

        // Limpiar lista
        documentosList.clear();

        // Obtener documentos subidos por el estudiante
        db.collection("documentos")
                .whereEqualTo("usuarioId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && isAdded()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Documento doc = document.toObject(Documento.class);
                            documentosList.add(doc);
                        }

                        actualizarUI();
                    }
                });

        // Obtener documentos compartidos con todos los estudiantes o con su carrera
        ProfileManager profileManager = new ProfileManager(requireContext());
        profileManager.obtenerPerfilActual(
                profile -> {
                    if (profile != null && isAdded()) {
                        String carrera = profile.getCareer();

                        db.collection("documentos")
                                .whereEqualTo("esPublico", true)
                                .get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful() && isAdded()) {
                                        for (QueryDocumentSnapshot document : task.getResult()) {
                                            Documento doc = document.toObject(Documento.class);

                                            // Verificar si es para todos o para su carrera
                                            if (doc.getCarreraDestino() == null ||
                                                    doc.getCarreraDestino().equals("Todas") ||
                                                    doc.getCarreraDestino().equals(carrera)) {
                                                documentosList.add(doc);
                                            }
                                        }

                                        actualizarUI();
                                    }
                                });
                    }
                },
                error -> {}
        );
    }

    private void actualizarUI() {
        if (documentosList.isEmpty()) {
            tvSinDocumentos.setVisibility(View.VISIBLE);
            recyclerDocumentos.setVisibility(View.GONE);
        } else {
            tvSinDocumentos.setVisibility(View.GONE);
            recyclerDocumentos.setVisibility(View.VISIBLE);
            documentosAdapter.notifyDataSetChanged();
        }
    }

    private void abrirDocumento(Documento documento) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(documento.getUrl()), "application/pdf");
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(Intent.createChooser(intent, "Abrir PDF"));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_DOCUMENT && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                subirDocumento(uri);
            }
        }
    }

    private void subirDocumento(Uri fileUri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();
        String fileName = getFileName(fileUri);

        // Crear referencia en Storage
        StorageReference fileRef = storage.getReference("documentos/" + userId + "/" + System.currentTimeMillis() + "_" + fileName);

        // Mostrar progreso (puedes implementar un ProgressBar aquí)
        Toast.makeText(requireContext(), "Subiendo documento...", Toast.LENGTH_SHORT).show();

        // Subir archivo
        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Obtener URL de descarga
                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        // Crear documento en Firestore
                        ProfileManager profileManager = new ProfileManager(requireContext());
                        profileManager.obtenerPerfilActual(
                                profile -> {
                                    if (profile != null && isAdded()) {
                                        Map<String, Object> docData = new HashMap<>();
                                        docData.put("nombre", fileName);
                                        docData.put("url", downloadUri.toString());
                                        docData.put("usuarioId", userId);
                                        docData.put("nombreUsuario", profile.getFullName());
                                        docData.put("fechaSubida", System.currentTimeMillis());
                                        docData.put("esPublico", false);
                                        docData.put("tipo", "Subido por estudiante");
                                        docData.put("carrera", profile.getCareer());

                                        db.collection("documentos")
                                                .add(docData)
                                                .addOnSuccessListener(documentReference -> {
                                                    if (isAdded()) {
                                                        Toast.makeText(requireContext(), "Documento subido correctamente", Toast.LENGTH_SHORT).show();
                                                        cargarDocumentos(); // Recargar documentos
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    if (isAdded()) {
                                                        Toast.makeText(requireContext(), "Error al registrar documento: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                },
                                error -> {
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(), "Error al obtener perfil: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                        );
                    });
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Error al subir documento: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    // Actualizar progreso (si implementas un ProgressBar)
                });
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}