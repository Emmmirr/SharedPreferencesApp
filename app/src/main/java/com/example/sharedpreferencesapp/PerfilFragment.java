package com.example.sharedpreferencesapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Fragment para gestionar el perfil de usuario
 * Versión completa con información personal detallada y manejo de errores
 *
 * @author anthonyllan
 * @version 3.0
 * @since 2025-07-15
 */
public class PerfilFragment extends Fragment {

    private static final String TAG = "PerfilFragment";
    private static final String PROFILE_IMAGE_FILE = "profile_image.jpg";

    // Views del perfil principal
    private ImageView ivProfileImage;
    private TextView tvDisplayName;
    private TextView tvEmail;
    private TextView tvAuthMethod;
    private TextView tvProfileCompleteness;
    private ProgressBar progressCompleteness;
    private Button btnEditProfile;
    private Button btnLogout;

    // Views de información personal detallada
    private TextView tvFullName;
    private TextView tvBirthDate;
    private TextView tvAccount;
    private TextView tvPasswordStatus;
    private Button btnChangePhoto;
    private Button btnChangePassword;

    // Manager
    private ProfileManager profileManager;
    private UserProfile currentProfile;

    // Launcher para seleccionar imagen
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public PerfilFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView iniciado por usuario: anthonyllan");

        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        try {
            initializeViews(view);
            setupImagePicker();
            setupEventListeners();

            profileManager = new ProfileManager(requireContext());
            cargarPerfilUsuario();

            Log.d(TAG, "PerfilFragment inicializado correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando PerfilFragment", e);
            Toast.makeText(getContext(), "Error al cargar el perfil", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void initializeViews(View view) {
        try {
            // Views principales
            ivProfileImage = view.findViewById(R.id.ivProfileImage);
            tvDisplayName = view.findViewById(R.id.tvDisplayName);
            tvEmail = view.findViewById(R.id.tvEmail);
            tvAuthMethod = view.findViewById(R.id.tvAuthMethod);
            tvProfileCompleteness = view.findViewById(R.id.tvProfileCompleteness);
            progressCompleteness = view.findViewById(R.id.progressCompleteness);
            btnEditProfile = view.findViewById(R.id.btnEditProfile);
            btnLogout = view.findViewById(R.id.btnLogout);

            // Views de información detallada
            tvFullName = view.findViewById(R.id.tvFullName);
            tvBirthDate = view.findViewById(R.id.tvBirthDate);
            tvAccount = view.findViewById(R.id.tvAccount);
            tvPasswordStatus = view.findViewById(R.id.tvPasswordStatus);
            btnChangePhoto = view.findViewById(R.id.btnChangePhoto);
            btnChangePassword = view.findViewById(R.id.btnChangePassword);

            Log.d(TAG, "Todas las vistas inicializadas correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error inicializando vistas", e);
        }
    }

    private void setupImagePicker() {
        try {
            imagePickerLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            if (imageUri != null) {
                                cargarImagenSeleccionada(imageUri);
                            }
                        }
                    }
            );
            Log.d(TAG, "Image picker configurado correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error configurando image picker", e);
        }
    }

    private void setupEventListeners() {
        try {
            if (btnEditProfile != null) {
                btnEditProfile.setOnClickListener(v -> mostrarDialogEditarPerfil());
            }
            if (btnLogout != null) {
                btnLogout.setOnClickListener(v -> mostrarDialogCerrarSesion());
            }
            if (ivProfileImage != null) {
                ivProfileImage.setOnClickListener(v -> seleccionarImagenPerfil());
            }

            // Nuevos listeners
            if (btnChangePhoto != null) {
                btnChangePhoto.setOnClickListener(v -> seleccionarImagenPerfil());
            }
            if (btnChangePassword != null) {
                btnChangePassword.setOnClickListener(v -> mostrarDialogCambiarPassword());
            }

            Log.d(TAG, "Event listeners configurados correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error configurando event listeners", e);
        }
    }

    private void cargarPerfilUsuario() {
        try {
            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
            String loginMethod = prefs.getString("loginMethod", "normal");

            Log.d(TAG, "Cargando perfil para método: " + loginMethod + " - Usuario: anthonyllan");

            if ("google".equals(loginMethod)) {
                // Usuario de Google - usar ProfileManager
                profileManager.obtenerPerfilActual(
                        profile -> {
                            currentProfile = profile;
                            actualizarVistasPerfil(profile);
                            Log.d(TAG, "Perfil de Google cargado exitosamente para: " + profile.getEmail());
                        },
                        error -> {
                            Log.e(TAG, "Error cargando perfil de Google", error);
                            mostrarPerfilUsuarioNormal();
                        }
                );
            } else {
                // Usuario normal - mostrar información básica
                mostrarPerfilUsuarioNormal();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cargando perfil de usuario", e);
            mostrarPerfilUsuarioNormal();
        }
    }

    private void mostrarPerfilUsuarioNormal() {
        try {
            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
            String email = prefs.getString("email", prefs.getString("username", "anthonyllan@ejemplo.com"));
            String username = prefs.getString("username", "anthonyllan");

            Log.d(TAG, "Mostrando perfil de usuario normal: " + email);

            // Crear un perfil temporal para usuarios normales
            UserProfile tempProfile = new UserProfile();
            tempProfile.setEmail(email);
            tempProfile.setDisplayName(username);
            tempProfile.setAuthMethod("normal");
            tempProfile.setActive(true);
            tempProfile.setProfileComplete(false);

            // Cargar datos adicionales si existen
            SharedPreferences profilePrefs = requireActivity().getSharedPreferences("UserProfilePrefs", android.content.Context.MODE_PRIVATE);
            String normalEmail = profilePrefs.getString("normal_user_email", "");
            if (!normalEmail.isEmpty() && normalEmail.equals(email)) {
                String displayName = profilePrefs.getString("normal_user_display_name", username);
                String firstName = profilePrefs.getString("normal_user_first_name", "");
                String lastName = profilePrefs.getString("normal_user_last_name", "");
                String birthDate = profilePrefs.getString("normal_user_birth_date", "");

                tempProfile.setDisplayName(displayName);
                tempProfile.setFirstName(firstName);
                tempProfile.setLastName(lastName);
                tempProfile.setDateOfBirth(birthDate);
            }

            currentProfile = tempProfile;
            actualizarVistasPerfil(tempProfile);
        } catch (Exception e) {
            Log.e(TAG, "Error mostrando perfil usuario normal", e);
        }
    }

    private void actualizarVistasPerfil(UserProfile profile) {
        try {
            if (profile == null) {
                Log.w(TAG, "Profile es null, no se puede actualizar");
                return;
            }

            // Información principal
            if (tvDisplayName != null) {
                tvDisplayName.setText(profile.getFullName().isEmpty() ? "anthonyllan" : profile.getFullName());
            }
            if (tvEmail != null) {
                tvEmail.setText(profile.getEmail());
            }

            // Información detallada
            if (tvFullName != null) {
                tvFullName.setText(profile.getFullName().isEmpty() ? "Sin especificar" : profile.getFullName());
            }
            if (tvBirthDate != null) {
                tvBirthDate.setText(profile.getDateOfBirth() != null && !profile.getDateOfBirth().isEmpty()
                        ? profile.getDateOfBirth() : "Sin especificar");
            }
            if (tvAccount != null) {
                tvAccount.setText(profile.getEmail());
            }

            // Estado de contraseña según método de auth
            if (tvPasswordStatus != null) {
                String passwordStatus = "google".equals(profile.getAuthMethod())
                        ? "Gestionada por Google" : "••••••••";
                tvPasswordStatus.setText(passwordStatus);
            }

            // Mostrar método de autenticación
            if (tvAuthMethod != null) {
                String authMethodText = "google".equals(profile.getAuthMethod()) ? "Google" : "Registro Normal";
                tvAuthMethod.setText("Autenticado con: " + authMethodText);
            }

            // Mostrar completitud del perfil
            int completeness = profile.getProfileCompleteness();
            if (tvProfileCompleteness != null) {
                tvProfileCompleteness.setText("Perfil completado: " + completeness + "%");
            }
            if (progressCompleteness != null) {
                progressCompleteness.setProgress(completeness);
            }

            // Cargar imagen de perfil desde almacenamiento local
            cargarImagenPerfil();

            Log.d(TAG, "Vistas de perfil actualizadas para: " + profile.getFullName());
        } catch (Exception e) {
            Log.e(TAG, "Error actualizando vistas del perfil", e);
        }
    }

    private void cargarImagenPerfil() {
        try {
            // Intentar cargar imagen guardada localmente
            FileInputStream fis = requireContext().openFileInput(PROFILE_IMAGE_FILE);
            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            fis.close();

            if (bitmap != null && ivProfileImage != null) {
                ivProfileImage.setImageBitmap(bitmap);
                Log.d(TAG, "Imagen de perfil cargada desde almacenamiento local");
            } else {
                setImagenPorDefecto();
            }
        } catch (Exception e) {
            Log.d(TAG, "No se encontró imagen de perfil, usando imagen por defecto");
            setImagenPorDefecto();
        }
    }

    private void setImagenPorDefecto() {
        try {
            if (ivProfileImage != null) {
                // Usar drawable por defecto según el método de autenticación
                if (currentProfile != null && "google".equals(currentProfile.getAuthMethod())) {
                    ivProfileImage.setImageResource(android.R.drawable.ic_menu_myplaces);
                } else {
                    ivProfileImage.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error estableciendo imagen por defecto", e);
        }
    }

    private void seleccionarImagenPerfil() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePickerLauncher.launch(Intent.createChooser(intent, "Seleccionar imagen de perfil"));
            Log.d(TAG, "Selector de imagen abierto");
        } catch (Exception e) {
            Log.e(TAG, "Error abriendo selector de imagen", e);
            Toast.makeText(getContext(), "Error al abrir selector de imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarImagenSeleccionada(Uri imageUri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();

                if (bitmap != null) {
                    // Redimensionar imagen para ahorrar espacio
                    Bitmap resizedBitmap = redimensionarBitmap(bitmap, 300, 300);

                    // Guardar imagen localmente
                    guardarImagenLocalmente(resizedBitmap);

                    // Mostrar en ImageView
                    if (ivProfileImage != null) {
                        ivProfileImage.setImageBitmap(resizedBitmap);
                    }

                    Toast.makeText(getContext(), "Imagen de perfil actualizada", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Imagen de perfil guardada exitosamente");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cargando imagen seleccionada", e);
            Toast.makeText(getContext(), "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap redimensionarBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        try {
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
        } catch (Exception e) {
            Log.e(TAG, "Error redimensionando bitmap", e);
            return bitmap; // Devolver original si hay error
        }
    }

    private void guardarImagenLocalmente(Bitmap bitmap) {
        try {
            FileOutputStream fos = requireContext().openFileOutput(PROFILE_IMAGE_FILE, android.content.Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            Log.d(TAG, "Imagen guardada en almacenamiento local");
        } catch (Exception e) {
            Log.e(TAG, "Error guardando imagen localmente", e);
        }
    }

    private void mostrarDialogCambiarPassword() {
        try {
            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
            String loginMethod = prefs.getString("loginMethod", "normal");

            if ("google".equals(loginMethod)) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Contraseña Gestionada por Google")
                        .setMessage("Tu contraseña es gestionada por Google. Para cambiarla, debes hacerlo desde tu cuenta de Google.")
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton("Entendido", null)
                        .show();
                return;
            }

            // Para usuarios normales - mostrar diálogo de cambio de contraseña
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_cambiar_password, null);
            builder.setView(dialogView);

            EditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
            EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
            EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            Button btnSave = dialogView.findViewById(R.id.btnSave);

            AlertDialog dialog = builder.create();

            if (btnCancel != null) {
                btnCancel.setOnClickListener(v -> dialog.dismiss());
            }

            if (btnSave != null) {
                btnSave.setOnClickListener(v -> {
                    if (etCurrentPassword == null || etNewPassword == null || etConfirmPassword == null) {
                        Toast.makeText(getContext(), "Error en el formulario", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String currentPassword = etCurrentPassword.getText().toString().trim();
                    String newPassword = etNewPassword.getText().toString().trim();
                    String confirmPassword = etConfirmPassword.getText().toString().trim();

                    // Validar contraseña actual
                    String storedPassword = prefs.getString("passwordCorrecto", "");
                    if (!currentPassword.equals(storedPassword)) {
                        Toast.makeText(getContext(), "Contraseña actual incorrecta", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Validar nueva contraseña
                    if (newPassword.length() < 6) {
                        Toast.makeText(getContext(), "La nueva contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!newPassword.equals(confirmPassword)) {
                        Toast.makeText(getContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Guardar nueva contraseña
                    prefs.edit().putString("passwordCorrecto", newPassword).apply();

                    Toast.makeText(getContext(), "Contraseña actualizada exitosamente", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    Log.d(TAG, "Contraseña actualizada para usuario: anthonyllan");
                });
            }

            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Error mostrando diálogo cambiar contraseña", e);
            Toast.makeText(getContext(), "Error al abrir el cambio de contraseña", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDialogEditarPerfil() {
        try {
            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
            String loginMethod = prefs.getString("loginMethod", "normal");

            Log.d(TAG, "Método de login detectado: " + loginMethod + " - Usuario: anthonyllan");

            if ("normal".equals(loginMethod)) {
                mostrarDialogEditarPerfilNormal();
                return;
            }

            // Para usuarios de Google
            if (currentProfile == null) {
                Log.e(TAG, "currentProfile es null, fallback a perfil normal");
                mostrarDialogEditarPerfilNormal();
                return;
            }

            // Verificar si el layout de Google existe
            try {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_editar_perfil, null);
                builder.setView(dialogView);

                setupDialogEditarPerfilGoogle(builder, dialogView);

            } catch (Exception e) {
                Log.e(TAG, "Error cargando dialog_editar_perfil para Google, usando simple", e);
                mostrarDialogEditarPerfilNormal();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error general en mostrarDialogEditarPerfil", e);
            Toast.makeText(getContext(), "Error al abrir el editor de perfil", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupDialogEditarPerfilGoogle(AlertDialog.Builder builder, View dialogView) {
        try {
            // Inicializar vistas del diálogo
            EditText etFirstName = dialogView.findViewById(R.id.etFirstName);
            EditText etLastName = dialogView.findViewById(R.id.etLastName);
            EditText etPhoneNumber = dialogView.findViewById(R.id.etPhoneNumber);
            EditText etJobTitle = dialogView.findViewById(R.id.etJobTitle);
            EditText etDepartment = dialogView.findViewById(R.id.etDepartment);
            EditText etInstitution = dialogView.findViewById(R.id.etInstitution);
            EditText etBio = dialogView.findViewById(R.id.etBio);
            EditText etDateOfBirth = dialogView.findViewById(R.id.etDateOfBirth);
            Spinner spinnerGender = dialogView.findViewById(R.id.spinnerGender);
            EditText etAddress = dialogView.findViewById(R.id.etAddress);
            EditText etCity = dialogView.findViewById(R.id.etCity);
            EditText etState = dialogView.findViewById(R.id.etState);
            EditText etCountry = dialogView.findViewById(R.id.etCountry);
            EditText etZipCode = dialogView.findViewById(R.id.etZipCode);

            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            Button btnSave = dialogView.findViewById(R.id.btnSave);

            // Configurar spinner de género
            String[] genders = {"", "Masculino", "Femenino", "Otro", "Prefiero no decir"};
            if (spinnerGender != null) {
                ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_spinner_item, genders);
                genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerGender.setAdapter(genderAdapter);
            }

            // Cargar datos actuales de forma segura
            if (currentProfile.getFirstName() != null && etFirstName != null)
                etFirstName.setText(currentProfile.getFirstName());
            if (currentProfile.getLastName() != null && etLastName != null)
                etLastName.setText(currentProfile.getLastName());
            if (currentProfile.getPhoneNumber() != null && etPhoneNumber != null)
                etPhoneNumber.setText(currentProfile.getPhoneNumber());
            if (currentProfile.getJobTitle() != null && etJobTitle != null)
                etJobTitle.setText(currentProfile.getJobTitle());
            if (currentProfile.getDepartment() != null && etDepartment != null)
                etDepartment.setText(currentProfile.getDepartment());
            if (currentProfile.getInstitution() != null && etInstitution != null)
                etInstitution.setText(currentProfile.getInstitution());
            if (currentProfile.getBio() != null && etBio != null)
                etBio.setText(currentProfile.getBio());
            if (currentProfile.getDateOfBirth() != null && etDateOfBirth != null)
                etDateOfBirth.setText(currentProfile.getDateOfBirth());
            if (currentProfile.getAddress() != null && etAddress != null)
                etAddress.setText(currentProfile.getAddress());
            if (currentProfile.getCity() != null && etCity != null)
                etCity.setText(currentProfile.getCity());
            if (currentProfile.getState() != null && etState != null)
                etState.setText(currentProfile.getState());
            if (currentProfile.getCountry() != null && etCountry != null)
                etCountry.setText(currentProfile.getCountry());
            if (currentProfile.getZipCode() != null && etZipCode != null)
                etZipCode.setText(currentProfile.getZipCode());

            // Seleccionar género actual
            if (currentProfile.getGender() != null && spinnerGender != null) {
                for (int i = 0; i < genders.length; i++) {
                    if (genders[i].equals(currentProfile.getGender())) {
                        spinnerGender.setSelection(i);
                        break;
                    }
                }
            }

            // Configurar DatePicker para fecha de nacimiento
            if (etDateOfBirth != null) {
                etDateOfBirth.setOnClickListener(v -> mostrarDatePicker(etDateOfBirth));
                etDateOfBirth.setFocusable(false);
            }

            AlertDialog dialog = builder.create();

            if (btnCancel != null) {
                btnCancel.setOnClickListener(v -> dialog.dismiss());
            }

            if (btnSave != null) {
                btnSave.setOnClickListener(v -> {
                    // Actualizar perfil con nuevos datos
                    if (etFirstName != null) currentProfile.setFirstName(etFirstName.getText().toString().trim());
                    if (etLastName != null) currentProfile.setLastName(etLastName.getText().toString().trim());
                    if (etPhoneNumber != null) currentProfile.setPhoneNumber(etPhoneNumber.getText().toString().trim());
                    if (etJobTitle != null) currentProfile.setJobTitle(etJobTitle.getText().toString().trim());
                    if (etDepartment != null) currentProfile.setDepartment(etDepartment.getText().toString().trim());
                    if (etInstitution != null) currentProfile.setInstitution(etInstitution.getText().toString().trim());
                    if (etBio != null) currentProfile.setBio(etBio.getText().toString().trim());
                    if (etDateOfBirth != null) currentProfile.setDateOfBirth(etDateOfBirth.getText().toString().trim());
                    if (spinnerGender != null) currentProfile.setGender(spinnerGender.getSelectedItem().toString());
                    if (etAddress != null) currentProfile.setAddress(etAddress.getText().toString().trim());
                    if (etCity != null) currentProfile.setCity(etCity.getText().toString().trim());
                    if (etState != null) currentProfile.setState(etState.getText().toString().trim());
                    if (etCountry != null) currentProfile.setCountry(etCountry.getText().toString().trim());
                    if (etZipCode != null) currentProfile.setZipCode(etZipCode.getText().toString().trim());

                    // Guardar cambios
                    profileManager.actualizarPerfilActual(currentProfile,
                            () -> {
                                Toast.makeText(getContext(), "Perfil actualizado exitosamente",
                                        Toast.LENGTH_SHORT).show();
                                actualizarVistasPerfil(currentProfile);
                                dialog.dismiss();
                                Log.d(TAG, "Perfil Google actualizado para: anthonyllan");
                            },
                            error -> {
                                Log.e(TAG, "Error actualizando perfil", error);
                                Toast.makeText(getContext(), "Error actualizando perfil: " + error.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                    );
                });
            }

            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Error en setupDialogEditarPerfilGoogle", e);
            Toast.makeText(getContext(), "Error configurando el diálogo", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDialogEditarPerfilNormal() {
        try {
            Log.d(TAG, "Mostrando diálogo de edición para usuario normal: anthonyllan");

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_editar_perfil_simple, null);

            if (dialogView == null) {
                Log.e(TAG, "dialogView es null, no se pudo inflar el layout");
                Toast.makeText(getContext(), "Error cargando el formulario", Toast.LENGTH_SHORT).show();
                return;
            }

            builder.setView(dialogView);

            // Verificar que las vistas existan antes de usarlas
            EditText etDisplayName = dialogView.findViewById(R.id.etDisplayName);
            EditText etFirstName = dialogView.findViewById(R.id.etFirstName);
            EditText etLastName = dialogView.findViewById(R.id.etLastName);
            EditText etDateOfBirth = dialogView.findViewById(R.id.etDateOfBirth);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            Button btnSave = dialogView.findViewById(R.id.btnSave);

            // Verificar que todas las vistas fueron encontradas
            if (etDisplayName == null || etFirstName == null || etLastName == null ||
                    etDateOfBirth == null || btnCancel == null || btnSave == null) {
                Log.e(TAG, "Error: No se pudieron encontrar todas las vistas en dialog_editar_perfil_simple");
                Log.e(TAG, "etDisplayName: " + (etDisplayName != null));
                Log.e(TAG, "etFirstName: " + (etFirstName != null));
                Log.e(TAG, "etLastName: " + (etLastName != null));
                Log.e(TAG, "etDateOfBirth: " + (etDateOfBirth != null));
                Log.e(TAG, "btnCancel: " + (btnCancel != null));
                Log.e(TAG, "btnSave: " + (btnSave != null));
                Toast.makeText(getContext(), "Error al cargar el formulario de edición", Toast.LENGTH_SHORT).show();
                return;
            }

            // Cargar datos actuales de forma segura
            if (currentProfile != null) {
                if (currentProfile.getDisplayName() != null) {
                    etDisplayName.setText(currentProfile.getDisplayName());
                }
                if (currentProfile.getFirstName() != null) {
                    etFirstName.setText(currentProfile.getFirstName());
                }
                if (currentProfile.getLastName() != null) {
                    etLastName.setText(currentProfile.getLastName());
                }
                if (currentProfile.getDateOfBirth() != null) {
                    etDateOfBirth.setText(currentProfile.getDateOfBirth());
                }
            }

            // Configurar DatePicker para fecha de nacimiento
            etDateOfBirth.setOnClickListener(v -> mostrarDatePicker(etDateOfBirth));
            etDateOfBirth.setFocusable(false);

            AlertDialog dialog = builder.create();

            btnCancel.setOnClickListener(v -> dialog.dismiss());

            btnSave.setOnClickListener(v -> {
                String newDisplayName = etDisplayName.getText().toString().trim();
                String firstName = etFirstName.getText().toString().trim();
                String lastName = etLastName.getText().toString().trim();
                String birthDate = etDateOfBirth.getText().toString().trim();

                if (!newDisplayName.isEmpty()) {
                    // Guardar en SharedPreferences para usuarios normales
                    SharedPreferences profilePrefs = requireActivity().getSharedPreferences("UserProfilePrefs", android.content.Context.MODE_PRIVATE);
                    profilePrefs.edit()
                            .putString("normal_user_display_name", newDisplayName)
                            .putString("normal_user_first_name", firstName)
                            .putString("normal_user_last_name", lastName)
                            .putString("normal_user_birth_date", birthDate)
                            .putString("normal_user_email", currentProfile.getEmail())
                            .apply();

                    currentProfile.setDisplayName(newDisplayName);
                    currentProfile.setFirstName(firstName);
                    currentProfile.setLastName(lastName);
                    currentProfile.setDateOfBirth(birthDate);
                    actualizarVistasPerfil(currentProfile);

                    Toast.makeText(getContext(), "Perfil actualizado exitosamente", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    Log.d(TAG, "Perfil normal actualizado para: anthonyllan");
                } else {
                    Toast.makeText(getContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                }
            });

            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Error en mostrarDialogEditarPerfilNormal", e);
            Toast.makeText(getContext(), "Error al mostrar el editor de perfil", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDatePicker(EditText editText) {
        try {
            Calendar calendar = Calendar.getInstance();

            String currentDate = editText.getText().toString();
            if (!currentDate.isEmpty()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    calendar.setTime(sdf.parse(currentDate));
                } catch (Exception e) {
                    Log.e(TAG, "Error parseando fecha actual", e);
                }
            }

            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                    (view, year, month, dayOfMonth) -> {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, dayOfMonth);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        editText.setText(sdf.format(selectedDate.getTime()));
                        Log.d(TAG, "Fecha seleccionada: " + sdf.format(selectedDate.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

            Calendar minDate = Calendar.getInstance();
            minDate.add(Calendar.YEAR, -100);
            datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());

            datePickerDialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Error mostrando date picker", e);
            Toast.makeText(getContext(), "Error al abrir selector de fecha", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDialogCerrarSesion() {
        try {
            new AlertDialog.Builder(getContext())
                    .setTitle("Cerrar Sesión")
                    .setMessage("¿Estás seguro que quieres cerrar sesión?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Sí, Cerrar Sesión", (dialog, which) -> {
                        try {
                            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
                            String loginMethod = prefs.getString("loginMethod", "normal");

                            // Limpiar SharedPreferences
                            prefs.edit().clear().apply();

                            // Cerrar sesión de Firebase si es usuario Google
                            if ("google".equals(loginMethod)) {
                                profileManager.cerrarSesion();
                            }

                            // Limpiar preferencias de perfil
                            SharedPreferences profilePrefs = requireActivity().getSharedPreferences("UserProfilePrefs", android.content.Context.MODE_PRIVATE);
                            profilePrefs.edit().clear().apply();

                            // Eliminar imagen de perfil
                            try {
                                requireContext().deleteFile(PROFILE_IMAGE_FILE);
                            } catch (Exception e) {
                                Log.d(TAG, "No se pudo eliminar imagen de perfil (normal si no existía)");
                            }

                            Log.d(TAG, "Sesión cerrada para método: " + loginMethod + " - Usuario: anthonyllan");

                            // Redirigir al LoginActivity
                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);

                            if (getActivity() != null) {
                                getActivity().finish();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error cerrando sesión", e);
                            Toast.makeText(getContext(), "Error al cerrar sesión", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error mostrando diálogo cerrar sesión", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "PerfilFragment resumed - Usuario activo: anthonyllan");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "PerfilFragment paused");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "PerfilFragment destroyed");
    }
}