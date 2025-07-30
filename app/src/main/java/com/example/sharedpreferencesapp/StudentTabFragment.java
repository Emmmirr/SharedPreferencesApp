package com.example.sharedpreferencesapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.developer.gbuttons.GoogleSignInButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentTabFragment extends Fragment {

    private static final String TAG = "StudentTabFragment";

    // Vista para login
    private View loginView;
    private EditText etStudentEmail, etStudentPassword;
    private Button btnStudentLogin;
    private GoogleSignInButton btnGoogleStudentLogin;
    private TextView tvGoToStudentSignup;

    // Vista para registro
    private View signupView;
    private EditText etStudentSignupEmail, etStudentSignupPassword, etStudentSignupConfirm, etStudentControlNumber;
    private Button btnStudentSignup;
    private GoogleSignInButton btnGoogleStudentSignup;
    private TextView tvGoToStudentLogin;
    private Spinner spinnerTeacher;  // Añadido: Spinner para seleccionar maestro
    private ProgressBar progressTeachers;  // Añadido: ProgressBar mientras se cargan los maestros

    private boolean isLoginView = true; // Por defecto mostramos login

    // Firebase Auth y Google Sign-In
    private FirebaseAuth auth;
    private GoogleSignInClient gClient;
    private ProfileManager profileManager;
    private TeacherService teacherService;  // Añadido: Servicio de maestros
    private ActivityResultLauncher<Intent> activityResultLauncher;

    // Lista de maestros disponibles
    private List<TeacherService.Teacher> availableTeachers = new ArrayList<>();

    public StudentTabFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_tab, container, false);

        initializeViews(view);
        initializeAuth();
        setupEventListeners();

        // Por defecto mostramos login
        showLoginView();

        return view;
    }

    private void initializeViews(View view) {
        // Inicializar las vistas de login y registro
        loginView = view.findViewById(R.id.student_login_view);
        signupView = view.findViewById(R.id.student_signup_view);

        // Inicializar campos de login
        etStudentEmail = view.findViewById(R.id.et_student_email);
        etStudentPassword = view.findViewById(R.id.et_student_password);
        btnStudentLogin = view.findViewById(R.id.btn_student_login);
        btnGoogleStudentLogin = view.findViewById(R.id.btn_google_student_login);
        tvGoToStudentSignup = view.findViewById(R.id.tv_go_to_student_signup);

        // Inicializar campos de registro
        etStudentSignupEmail = view.findViewById(R.id.et_student_signup_email);
        etStudentSignupPassword = view.findViewById(R.id.et_student_signup_password);
        etStudentSignupConfirm = view.findViewById(R.id.et_student_signup_confirm);
        etStudentControlNumber = view.findViewById(R.id.et_student_control_number);
        btnStudentSignup = view.findViewById(R.id.btn_student_signup);
        btnGoogleStudentSignup = view.findViewById(R.id.btn_google_student_signup);
        tvGoToStudentLogin = view.findViewById(R.id.tv_go_to_student_login);

        // Inicializar spinner de maestros y progreso
        spinnerTeacher = view.findViewById(R.id.spinner_teacher);
        progressTeachers = view.findViewById(R.id.progress_teachers);
    }

    private void initializeAuth() {
        auth = FirebaseAuth.getInstance();
        profileManager = new ProfileManager(requireContext());
        teacherService = new TeacherService();  // Inicializar servicio de maestros

        GoogleSignInOptions gOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        gClient = GoogleSignIn.getClient(requireActivity(), gOptions);

        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleGoogleSignInResult);
    }

    private void setupEventListeners() {
        // Evento de login con email/password
        btnStudentLogin.setOnClickListener(v -> {
            String email = etStudentEmail.getText().toString().trim();
            String password = etStudentPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Por favor completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            loginEstudianteConEmail(email, password);
        });

        // Evento de login con Google
        btnGoogleStudentLogin.setOnClickListener(v -> {
            Intent signInIntent = gClient.getSignInIntent();
            activityResultLauncher.launch(signInIntent);
        });

        // Cambiar a vista de registro
        tvGoToStudentSignup.setOnClickListener(v -> showSignupView());

        // Evento de registro con email/password
        btnStudentSignup.setOnClickListener(v -> {
            String email = etStudentSignupEmail.getText().toString().trim();
            String password = etStudentSignupPassword.getText().toString().trim();
            String confirmPassword = etStudentSignupConfirm.getText().toString().trim();
            String controlNumber = etStudentControlNumber.getText().toString().trim();

            if (!validateStudentForm(email, password, confirmPassword, controlNumber)) {
                return;
            }

            // Verificar que se ha seleccionado un maestro
            if (spinnerTeacher.getSelectedItemPosition() <= 0) {
                Toast.makeText(getContext(), "Por favor selecciona un maestro supervisor", Toast.LENGTH_SHORT).show();
                return;
            }

            // Obtener el maestro seleccionado
            TeacherService.Teacher selectedTeacher = availableTeachers.get(spinnerTeacher.getSelectedItemPosition() - 1);

            registrarEstudianteConEmail(email, password, controlNumber, selectedTeacher);
        });

        // Evento de registro con Google
        btnGoogleStudentSignup.setOnClickListener(v -> {
            String controlNumber = etStudentControlNumber.getText().toString().trim();
            if (controlNumber.isEmpty()) {
                Toast.makeText(getContext(), "El número de control es obligatorio", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verificar que se ha seleccionado un maestro
            if (spinnerTeacher.getSelectedItemPosition() <= 0) {
                Toast.makeText(getContext(), "Por favor selecciona un maestro supervisor", Toast.LENGTH_SHORT).show();
                return;
            }

            // Obtener el maestro seleccionado
            TeacherService.Teacher selectedTeacher = availableTeachers.get(spinnerTeacher.getSelectedItemPosition() - 1);

            // Guardar temporalmente el número de control y maestro para usarlos después del login con Google
            SharedPreferences prefs = requireActivity().getSharedPreferences("TempStudentData", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("tempControlNumber", controlNumber);
            editor.putString("tempSupervisorId", selectedTeacher.getId());
            editor.putString("tempSupervisorName", selectedTeacher.getFullName());
            editor.apply();

            Intent signInIntent = gClient.getSignInIntent();
            activityResultLauncher.launch(signInIntent);
        });

        // Cambiar a vista de login
        tvGoToStudentLogin.setOnClickListener(v -> showLoginView());
    }

    private void showLoginView() {
        isLoginView = true;
        loginView.setVisibility(View.VISIBLE);
        signupView.setVisibility(View.GONE);
    }

    private void showSignupView() {
        isLoginView = false;
        loginView.setVisibility(View.GONE);
        signupView.setVisibility(View.VISIBLE);

        // Cargar maestros cuando se muestra la vista de registro
        loadAvailableTeachers();
    }

    private void loadAvailableTeachers() {
        progressTeachers.setVisibility(View.VISIBLE);

        Log.d(TAG, "Iniciando carga de maestros...");

        teacherService.getAvailableTeachers(new TeacherService.OnTeachersLoadedListener() {
            @Override
            public void onTeachersLoaded(List<TeacherService.Teacher> teachers) {
                if (getContext() == null) return;

                Log.d(TAG, "Maestros cargados: " + teachers.size());

                availableTeachers = teachers;

                // Crear lista para el spinner añadiendo un ítem inicial "Seleccione un maestro"
                List<String> teacherNames = new ArrayList<>();
                teacherNames.add("Seleccione un maestro");

                for (TeacherService.Teacher teacher : teachers) {
                    Log.d(TAG, "Añadiendo maestro: " + teacher.getFullName() + ", ID: " + teacher.getId());
                    teacherNames.add(teacher.getFullName());
                }

                // Si no hay maestros, añadir un mensaje
                if (teachers.isEmpty()) {
                    teacherNames.add("No hay maestros disponibles");
                    // Opcionalmente, crear un maestro de prueba para desarrollo
                    // teacherService.crearMaestrosDePrueba();
                }

                // Configurar el adapter para el spinner
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        getContext(),
                        android.R.layout.simple_spinner_item,
                        teacherNames
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerTeacher.setAdapter(adapter);

                progressTeachers.setVisibility(View.GONE);
            }

            @Override
            public void onError(Exception e) {
                if (getContext() == null) return;

                Log.e(TAG, "Error cargando maestros", e);
                Toast.makeText(getContext(), "Error al cargar la lista de maestros: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                progressTeachers.setVisibility(View.GONE);

                // Poner un adaptador con mensaje de error
                List<String> emptyList = new ArrayList<>();
                emptyList.add("Error al cargar maestros");
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        getContext(),
                        android.R.layout.simple_spinner_item,
                        emptyList
                );
                spinnerTeacher.setAdapter(adapter);
            }
        });
    }

    private boolean validateStudentForm(String email, String password, String confirmPassword, String controlNumber) {
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || controlNumber.isEmpty()) {
            Toast.makeText(getContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getContext(), "Ingresa un email válido", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (password.length() < 6) {
            Toast.makeText(getContext(), "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(getContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (controlNumber.length() != 8) {
            Toast.makeText(getContext(), "El número de control debe tener 8 dígitos", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void loginEstudianteConEmail(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        verificarPerfilEstudiante(user);
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(getContext(), "Error al iniciar sesión: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registrarEstudianteConEmail(String email, String password, String controlNumber, TeacherService.Teacher selectedTeacher) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = auth.getCurrentUser();
                        crearPerfilEstudiante(user, controlNumber, email, selectedTeacher.getId(), selectedTeacher.getFullName());
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(getContext(), "Error en el registro: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleGoogleSignInResult(ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.e(TAG, "Error en Google Sign-In", e);
                Toast.makeText(getActivity(), "Error con Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            if (!isLoginView) {
                                // Es registro, necesitamos el número de control y maestro
                                SharedPreferences prefs = requireActivity().getSharedPreferences("TempStudentData", Context.MODE_PRIVATE);
                                String controlNumber = prefs.getString("tempControlNumber", "");
                                String supervisorId = prefs.getString("tempSupervisorId", "");
                                String supervisorName = prefs.getString("tempSupervisorName", "");

                                crearPerfilEstudiante(user, controlNumber, user.getEmail(), supervisorId, supervisorName);

                                // Limpiar datos temporales
                                prefs.edit().clear().apply();
                            } else {
                                // Es login, verificar si ya está registrado como estudiante
                                verificarPerfilEstudiante(user);
                            }
                        }
                    } else {
                        Log.e(TAG, "Firebase auth falló", task.getException());
                        Toast.makeText(getContext(), "Error en la autenticación: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void crearPerfilEstudiante(FirebaseUser user, String controlNumber, String email, String supervisorId, String supervisorName) {
        if (user == null) return;

        // Crear un perfil básico con tipo "student"
        UserProfile newProfile = new UserProfile(user.getUid(), email,
                user.getDisplayName() != null ? user.getDisplayName() : email,
                "email");
        newProfile.setControlNumber(controlNumber);
        newProfile.setUserType("student"); // Campo para distinguir entre admin y estudiante

        // Añadir información del supervisor
        newProfile.setSupervisorId(supervisorId);
        newProfile.setSupervisorName(supervisorName);
        newProfile.setApproved(false); // Por defecto no está aprobado

        profileManager.crearOVerificarPerfil(user, email, "email",
                () -> {
                    // Ahora actualizamos con los datos específicos del estudiante
                    profileManager.actualizarPerfilActual(newProfile,
                            () -> {
                                Toast.makeText(getContext(), "¡Registro exitoso!", Toast.LENGTH_SHORT).show();
                                guardarSesionLocalYRedirigir(email, email, "student");
                            },
                            e -> {
                                Log.e(TAG, "Error actualizando perfil de estudiante", e);
                                Toast.makeText(getContext(), "Error creando perfil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                },
                e -> {
                    Log.e(TAG, "Error creando perfil en Firestore", e);
                    Toast.makeText(getContext(), "Error en el registro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void verificarPerfilEstudiante(FirebaseUser user) {
        if (user == null) return;

        profileManager.obtenerPerfilActual(
                profile -> {
                    // Verificar si es un perfil de estudiante
                    String userType = profile.getUserType();
                    if (userType == null || !userType.equals("student")) {
                        Toast.makeText(getContext(), "Esta cuenta no está registrada como estudiante", Toast.LENGTH_LONG).show();
                        auth.signOut(); // Cerrar sesión si no es estudiante
                        return;
                    }

                    Toast.makeText(getContext(), "¡Bienvenido de nuevo!", Toast.LENGTH_SHORT).show();
                    guardarSesionLocalYRedirigir(profile.getDisplayName(), profile.getEmail(), "student");
                },
                e -> {
                    Log.e(TAG, "Error verificando perfil de estudiante", e);
                    Toast.makeText(getContext(), "Error al iniciar sesión: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    auth.signOut();
                }
        );
    }

    private void guardarSesionLocalYRedirigir(String username, String email, String userType) {
        if (getActivity() == null) return;

        SharedPreferences prefs = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("username", username);
        editor.putString("email", email);
        editor.putString("userType", userType); // Guardar tipo de usuario
        editor.apply();

        Intent intent;
        if ("student".equals(userType)) {
            intent = new Intent(getActivity(), StudentMainActivity.class); // Nueva actividad para estudiantes
        } else {
            intent = new Intent(getActivity(), MainActivity.class); // Actividad existente para admin
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }
}