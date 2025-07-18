package com.example.sharedpreferencesapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PerfilFragment extends Fragment {

    private static final String TAG = "PerfilFragment";
    private static final String PROFILE_IMAGE_PREFIX = "profile_image_";
    private static final String KEY_CAMERA_URI = "key_camera_uri";

    // Vistas
    private ImageView ivProfileImage;
    private TextView tvDisplayName, tvEmail, tvAuthMethod, tvProfileCompleteness;
    private TextView tvFullName, tvBirthDateAndAge, tvAccount, tvPasswordStatus;
    private Button btnEditProfile, btnLogout, btnChangePhoto, btnChangePassword, btnExportPdf;
    private ProgressBar progressCompleteness;

    // Managers y estado
    private ProfileManager profileManager;
    private UserProfile currentProfile;
    private Uri cameraImageUri;

    // Launchers
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> inePickerLauncher;
    private ActivityResultLauncher<Intent> pdfSaveLauncher;

    public PerfilFragment() {}

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (cameraImageUri != null) {
            outState.putParcelable(KEY_CAMERA_URI, cameraImageUri);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        if (savedInstanceState != null) {
            cameraImageUri = savedInstanceState.getParcelable(KEY_CAMERA_URI);
        }

        initializeViews(view);
        setupLaunchers();
        setupEventListeners();

        profileManager = new ProfileManager(requireContext());
        cargarPerfilUsuario();

        return view;
    }

    private void initializeViews(View view) {
        ivProfileImage = view.findViewById(R.id.ivProfileImage);
        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvAuthMethod = view.findViewById(R.id.tvAuthMethod);
        tvProfileCompleteness = view.findViewById(R.id.tvProfileCompleteness);
        progressCompleteness = view.findViewById(R.id.progressCompleteness);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnLogout = view.findViewById(R.id.btnLogout);
        tvFullName = view.findViewById(R.id.tvFullName);
        tvBirthDateAndAge = view.findViewById(R.id.tvBirthDate);
        tvAccount = view.findViewById(R.id.tvAccount);
        tvPasswordStatus = view.findViewById(R.id.tvPasswordStatus);
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        btnExportPdf = view.findViewById(R.id.btnExportPdf);
    }

    private void setupLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                isSuccess -> {
                    if (isSuccess && cameraImageUri != null) {
                        copiarUriAAlmacenamientoInterno(cameraImageUri);
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        copiarUriAAlmacenamientoInterno(uri);
                    }
                }
        );

        inePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        Toast.makeText(getContext(), "Subiendo credencial...", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        pdfSaveLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            generarYGuardarPDF(uri);
                        }
                    }
                }
        );
    }

    private void setupEventListeners() {
        btnEditProfile.setOnClickListener(v -> mostrarDialogEditarPerfil());
        btnLogout.setOnClickListener(v -> mostrarDialogCerrarSesion());
        ivProfileImage.setOnClickListener(v -> mostrarDialogElegirFoto());
        btnChangePhoto.setOnClickListener(v -> mostrarDialogElegirFoto());
        btnChangePassword.setOnClickListener(v -> mostrarDialogCambiarPassword());
        if (btnExportPdf != null) {
            btnExportPdf.setOnClickListener(v -> exportarPerfilAPDF());
        }
    }

    private void cargarPerfilUsuario() {
        profileManager.obtenerPerfilActual(
                profile -> {
                    this.currentProfile = profile;
                    actualizarVistasPerfil(profile);
                },
                error -> {
                    Log.e(TAG, "Error al cargar el perfil desde Firestore", error);
                    Toast.makeText(getContext(), "No se pudo cargar el perfil.", Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void actualizarVistasPerfil(UserProfile profile) {
        if (profile == null || getContext() == null) return;
        tvDisplayName.setText(profile.getFullName().isEmpty() ? profile.getDisplayName() : profile.getFullName());
        tvEmail.setText(profile.getEmail());
        String authMethodText = "google".equals(profile.getAuthMethod()) ? "Google" : "Email y Contraseña";
        tvAuthMethod.setText("Autenticado con: " + authMethodText);
        tvFullName.setText(profile.getFullName().isEmpty() ? "Sin especificar" : profile.getFullName());
        String birthDateText = profile.getDateOfBirth().isEmpty() ? "Sin especificar" : profile.getDateOfBirth();
        int age = profile.getAge();
        if (age > 0) {
            tvBirthDateAndAge.setText(birthDateText + " (" + age + " años)");
        } else {
            tvBirthDateAndAge.setText(birthDateText);
        }
        tvAccount.setText(profile.getEmail());
        String passwordStatus = "google".equals(profile.getAuthMethod()) ? "Gestionada por Google" : "••••••••";
        tvPasswordStatus.setText(passwordStatus);
        int completeness = profile.getProfileCompleteness();
        tvProfileCompleteness.setText("Perfil completado: " + completeness + "%");
        progressCompleteness.setProgress(completeness);

        cargarImagenPerfilLocal();
    }

    private void mostrarDialogElegirFoto() {
        new AlertDialog.Builder(getContext())
                .setTitle("Foto de Perfil")
                .setItems(new String[]{"Tomar foto con la cámara", "Seleccionar de la galería"}, (dialog, which) -> {
                    if (which == 0) {
                        abrirCamara();
                    } else {
                        galleryLauncher.launch("image/*");
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void abrirCamara() {
        try {
            File photoFile = crearArchivoDeImagenTemporal();
            cameraImageUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getApplicationContext().getPackageName() + ".provider",
                    photoFile);
            cameraLauncher.launch(cameraImageUri);
        } catch (IOException e) {
            Log.e(TAG, "Error al crear archivo para la foto", e);
            Toast.makeText(getContext(), "No se pudo preparar la cámara.", Toast.LENGTH_SHORT).show();
        }
    }

    private File crearArchivoDeImagenTemporal() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void copiarUriAAlmacenamientoInterno(Uri sourceUri) {
        if (getContext() == null || currentProfile == null) return;
        String fileName = getProfileImageFilename();
        try (InputStream inputStream = getContext().getContentResolver().openInputStream(sourceUri);
             FileOutputStream fos = getContext().openFileOutput(fileName, Context.MODE_PRIVATE)) {
            if (inputStream == null) return;
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            Bitmap resizedBitmap = redimensionarBitmap(bitmap, 400, 400);
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            ivProfileImage.setImageBitmap(resizedBitmap);
            Toast.makeText(getContext(), "Foto de perfil actualizada.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error al copiar la imagen", e);
            Toast.makeText(getContext(), "No se pudo guardar la imagen.", Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarImagenPerfilLocal() {
        if (getContext() == null || currentProfile == null) return;
        String fileName = getProfileImageFilename();
        try {
            File profileImageFile = new File(getContext().getFilesDir(), fileName);
            if (profileImageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(profileImageFile.getAbsolutePath());
                ivProfileImage.setImageBitmap(bitmap);
            } else {
                ivProfileImage.setImageResource(R.drawable.user);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al cargar imagen local", e);
            ivProfileImage.setImageResource(R.drawable.user);
        }
    }

    private String getProfileImageFilename() {
        if (currentProfile == null || currentProfile.getUserId().isEmpty()) {
            return PROFILE_IMAGE_PREFIX + "default.jpg";
        }
        return PROFILE_IMAGE_PREFIX + currentProfile.getUserId() + ".jpg";
    }

    private void exportarPerfilAPDF() {
        if (currentProfile == null) {
            Toast.makeText(getContext(), "No hay datos de perfil para exportar.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        String fileName = "Perfil_" + currentProfile.getFullName().replace(" ", "_") + ".pdf";
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        pdfSaveLauncher.launch(intent);
    }

    private void generarYGuardarPDF(Uri uri) {
        try (OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri)) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(36, 36, 36, 36);

            crearContenidoPdf(document, currentProfile);

            document.close();
            Toast.makeText(getContext(), "PDF guardado exitosamente.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error al guardar PDF", e);
            Toast.makeText(getContext(), "Error al guardar el PDF.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- MÉTODO DE PDF ESTILIZADO ---
    private void crearContenidoPdf(Document document, UserProfile profile) throws IOException {
        // Definir fuentes y colores
        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        Color headerColor = new DeviceRgb(66, 153, 225); // Un azul similar al de tu app

        // Título Principal
        document.add(new Paragraph("Perfil de Usuario")
                .setFont(boldFont).setFontSize(22).setFontColor(headerColor)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

        // Separador
        document.add(new LineSeparator(new SolidLine(1f)).setMarginBottom(15));

        // --- SECCIONES ---
        document.add(createSectionHeader("Información Personal", boldFont));
        document.add(createProfileEntry("Nombre Completo:", profile.getFullName(), regularFont));
        document.add(createProfileEntry("Fecha de Nacimiento:", profile.getDateOfBirth(), regularFont));
        document.add(createProfileEntry("Edad:", profile.getAge() > 0 ? profile.getAge() + " años" : "No especificada", regularFont));
        document.add(createProfileEntry("Género:", profile.getGender(), regularFont));
        document.add(createProfileEntry("CURP:", profile.getCurp(), regularFont));

        document.add(createSectionHeader("Información Académica", boldFont));
        document.add(createProfileEntry("Carrera:", profile.getCareer(), regularFont));
        document.add(createProfileEntry("Número de Control:", profile.getControlNumber(), regularFont));

        document.add(createSectionHeader("Datos de Contacto", boldFont));
        document.add(createProfileEntry("Email:", profile.getEmail(), regularFont));
        document.add(createProfileEntry("Teléfono:", profile.getPhoneNumber(), regularFont));
        document.add(createProfileEntry("Contacto de Emergencia:", profile.getEmergencyContactName() + " - " + profile.getEmergencyContactPhone(), regularFont));

        document.add(createSectionHeader("Condiciones Médicas", boldFont));
        document.add(new Paragraph(profile.getMedicalConditions().isEmpty() ? "Ninguna especificada." : profile.getMedicalConditions())
                .setFont(regularFont).setFontSize(11).setMarginBottom(5));

        // Pie de página
        String dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        document.add(new Paragraph("Generado el: " + dateTime)
                .setFont(regularFont).setFontSize(8)
                .setTextAlignment(TextAlignment.RIGHT).setItalic().setMarginTop(40));
    }

    private Paragraph createSectionHeader(String text, PdfFont font) {
        return new Paragraph(text)
                .setFont(font).setFontSize(16).setBold()
                .setUnderline(0.5f, -1.5f)
                .setMarginTop(15).setMarginBottom(8);
    }

    private Paragraph createProfileEntry(String label, String value, PdfFont font) {
        if (value == null || value.trim().isEmpty()) {
            value = "No especificado";
        }
        return new Paragraph()
                .add(new Text(label).setBold())
                .add(" " + value)
                .setFont(font).setFontSize(11)
                .setMarginBottom(4);
    }

    private void mostrarDialogEditarPerfil() {
        if (currentProfile == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_editar_perfil, null);
        builder.setView(dialogView);

        EditText etFullName = dialogView.findViewById(R.id.etFullName);
        EditText etDateOfBirth = dialogView.findViewById(R.id.etDateOfBirth);
        Spinner spinnerGender = dialogView.findViewById(R.id.spinnerGender);
        EditText etCurp = dialogView.findViewById(R.id.etCurp);
        Button btnUploadIne = dialogView.findViewById(R.id.btnUploadIne);
        Spinner spinnerCareer = dialogView.findViewById(R.id.spinnerCareer);
        EditText etControlNumber = dialogView.findViewById(R.id.etControlNumber);
        EditText etMedicalConditions = dialogView.findViewById(R.id.etMedicalConditions);
        EditText etPhoneNumber = dialogView.findViewById(R.id.etPhoneNumber);
        EditText etEmergencyContactName = dialogView.findViewById(R.id.etEmergencyContactName);
        EditText etEmergencyContactPhone = dialogView.findViewById(R.id.etEmergencyContactPhone);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        String[] genders = {"", "Masculino", "Femenino", "Otro"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, genders);
        spinnerGender.setAdapter(genderAdapter);
        String[] careers = {"INGENIERIA EN SISTEMAS COMPUTACIONALES", "INGENIERIA CIVIL", "INGENIERIA EN GESTION EMPRESARIAL", "INGENIERIA EN INFORMATICA", "CONTABILIDAD"};
        ArrayAdapter<String> careerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, careers);
        spinnerCareer.setAdapter(careerAdapter);

        etFullName.setText(currentProfile.getFullName());
        etDateOfBirth.setText(currentProfile.getDateOfBirth());
        etCurp.setText(currentProfile.getCurp());
        etControlNumber.setText(currentProfile.getControlNumber());
        etMedicalConditions.setText(currentProfile.getMedicalConditions());
        etPhoneNumber.setText(currentProfile.getPhoneNumber());
        etEmergencyContactName.setText(currentProfile.getEmergencyContactName());
        etEmergencyContactPhone.setText(currentProfile.getEmergencyContactPhone());
        for (int i = 0; i < genders.length; i++) { if (genders[i].equals(currentProfile.getGender())) { spinnerGender.setSelection(i); break; } }
        for (int i = 0; i < careers.length; i++) { if (careers[i].equals(currentProfile.getCareer())) { spinnerCareer.setSelection(i); break; } }

        etDateOfBirth.setOnClickListener(v -> mostrarDatePicker(etDateOfBirth));
        etDateOfBirth.setFocusable(false);
        btnUploadIne.setOnClickListener(v -> inePickerLauncher.launch("image/*"));

        AlertDialog dialog = builder.create();
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            if (etFullName.getText().toString().trim().isEmpty() ||
                    etPhoneNumber.getText().toString().trim().isEmpty() ||
                    etDateOfBirth.getText().toString().trim().isEmpty()) {
                Toast.makeText(getContext(), "Nombre, teléfono y fecha de nacimiento son requeridos.", Toast.LENGTH_SHORT).show();
                return;
            }
            currentProfile.setFullName(etFullName.getText().toString().trim());
            currentProfile.setDateOfBirth(etDateOfBirth.getText().toString().trim());
            currentProfile.setGender(spinnerGender.getSelectedItem().toString());
            currentProfile.setCurp(etCurp.getText().toString().trim());
            currentProfile.setCareer(spinnerCareer.getSelectedItem().toString());
            currentProfile.setControlNumber(etControlNumber.getText().toString().trim());
            currentProfile.setMedicalConditions(etMedicalConditions.getText().toString().trim());
            currentProfile.setPhoneNumber(etPhoneNumber.getText().toString().trim());
            currentProfile.setEmergencyContactName(etEmergencyContactName.getText().toString().trim());
            currentProfile.setEmergencyContactPhone(etEmergencyContactPhone.getText().toString().trim());

            profileManager.actualizarPerfilActual(currentProfile,
                    () -> {
                        Toast.makeText(getContext(), "Perfil actualizado.", Toast.LENGTH_SHORT).show();
                        actualizarVistasPerfil(currentProfile);
                        dialog.dismiss();
                    },
                    error -> Toast.makeText(getContext(), "Error al guardar: " + error.getMessage(), Toast.LENGTH_SHORT).show()
            );
        });
        dialog.show();
    }

    private void mostrarDialogCerrarSesion() {
        new AlertDialog.Builder(getContext())
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro que quieres cerrar sesión?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Sí, Cerrar Sesión", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    SharedPreferences loginPrefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                    loginPrefs.edit().clear().apply();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogCambiarPassword() {
        if (currentProfile == null || getContext() == null) return;
        if ("google".equals(currentProfile.getAuthMethod())) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Contraseña Gestionada por Google")
                    .setMessage("Tu contraseña es gestionada por Google.")
                    .setPositiveButton("Entendido", null)
                    .show();
        } else {
            new AlertDialog.Builder(getContext())
                    .setTitle("Cambiar Contraseña")
                    .setMessage("Se enviará un correo a " + currentProfile.getEmail() + " para restablecer tu contraseña. ¿Deseas continuar?")
                    .setPositiveButton("Sí, Enviar", (dialog, which) -> {
                        FirebaseAuth.getInstance().sendPasswordResetEmail(currentProfile.getEmail())
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(getContext(), "Correo de restablecimiento enviado.", Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(getContext(), "Error al enviar el correo.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
    }

    private void mostrarDatePicker(EditText editText) {
        if (getContext() == null) return;
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    editText.setText(sdf.format(selectedDate.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private Bitmap redimensionarBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float ratioBitmap = (float) width / (float) height;
        float ratioMax = (float) maxWidth / (float) maxHeight;
        int finalWidth = maxWidth;
        int finalHeight = maxHeight;
        if (ratioMax > ratioBitmap) {
            finalWidth = (int) ((float) maxHeight * ratioBitmap);
        } else {
            finalHeight = (int) ((float) maxWidth / ratioBitmap);
        }
        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true);
    }
}
