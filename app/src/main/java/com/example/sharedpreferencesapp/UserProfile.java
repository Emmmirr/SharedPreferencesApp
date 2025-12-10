package com.example.sharedpreferencesapp;

import android.util.Log;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Clase POJO (Plain Old Java Object) que representa el perfil de un usuario.
 * Contiene todos los campos necesarios para la nueva sección de perfil.
 */
public class UserProfile {

    // --- Datos de Identificación (Esenciales) ---
    private String userId = "";
    private String email = "";
    private String displayName = "";
    private String authMethod = "";

    // --- Información Personal ---
    private String fullName = "";
    private String dateOfBirth = "";
    private String gender = "";
    private String curp = "";
    private String ineScanUrl = "";
    private String career = "";
    private String controlNumber = "";
    private String medicalConditions = "";

    // --- Datos de Contacto ---
    private String phoneNumber = "";
    private String emergencyContactName = "";
    private String emergencyContactPhone = "";

    // --- Fotografía del Usuario ---
    // Este es el campo que usaremos para guardar la URL de Firebase Storage. ¡Ya está listo!
    private String profileImageUrl = "";

    // --- Metadatos ---
    private String createdAt = "";
    private String updatedAt = "";
    private boolean isProfileComplete = false;

    private String userType = ""; // "admin", "student" o "administrator"

    // --- Datos de relación maestro-alumno ---
    private String supervisorId = ""; // ID del maestro supervisor
    private String supervisorName = ""; // Nombre del maestro para mostrar
    private boolean isApproved = false; // Indica si el maestro ha aprobado al alumno

    // Constructor vacío requerido por Firestore
    public UserProfile() {}

    // Constructor para un nuevo perfil
    public UserProfile(String userId, String email, String displayName, String authMethod) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.authMethod = authMethod;
        this.createdAt = String.valueOf(System.currentTimeMillis());
    }

    // --- Métodos de Conversión para Firestore ---

    public static UserProfile fromMap(Map<String, Object> map) {
        UserProfile profile = new UserProfile();
        if (map == null) return profile;

        // Helper method para obtener string seguro (maneja null y valores faltantes)
        profile.userId = getStringFromMap(map, "userId", "");
        profile.email = getStringFromMap(map, "email", "");
        profile.displayName = getStringFromMap(map, "displayName", "");
        profile.authMethod = getStringFromMap(map, "authMethod", "");
        profile.fullName = getStringFromMap(map, "fullName", "");
        profile.dateOfBirth = getStringFromMap(map, "dateOfBirth", "");
        profile.gender = getStringFromMap(map, "gender", "");
        profile.curp = getStringFromMap(map, "curp", "");
        profile.ineScanUrl = getStringFromMap(map, "ineScanUrl", "");
        profile.career = getStringFromMap(map, "career", "");
        profile.controlNumber = getStringFromMap(map, "controlNumber", "");
        profile.medicalConditions = getStringFromMap(map, "medicalConditions", "");
        profile.phoneNumber = getStringFromMap(map, "phoneNumber", "");
        profile.emergencyContactName = getStringFromMap(map, "emergencyContactName", "");
        profile.emergencyContactPhone = getStringFromMap(map, "emergencyContactPhone", "");
        profile.profileImageUrl = getStringFromMap(map, "profileImageUrl", "");
        profile.createdAt = getStringFromMap(map, "createdAt", "");
        profile.updatedAt = getStringFromMap(map, "updatedAt", "");
        profile.isProfileComplete = getBooleanFromMap(map, "isProfileComplete", false);
        profile.userType = getStringFromMap(map, "userType", "");

        // Nuevos campos añadidos
        profile.supervisorId = getStringFromMap(map, "supervisorId", "");
        profile.supervisorName = getStringFromMap(map, "supervisorName", "");
        profile.isApproved = getBooleanFromMap(map, "isApproved", false);

        // Log para debug
        Log.d("UserProfile", "fromMap - email: " + profile.email + ", fullName: " + profile.fullName + ", displayName: " + profile.displayName);

        return profile;
    }

    // Helper method para obtener strings de forma segura
    private static String getStringFromMap(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof String) {
            String str = (String) value;
            // Log para campos importantes
            if (key.equals("fullName") || key.equals("email") || key.equals("displayName") ||
                    key.equals("controlNumber") || key.equals("career")) {
                Log.d("UserProfile", "getStringFromMap - " + key + ": '" + str + "' (length: " + str.length() + ")");
            }
            return str; // Devolver el string tal cual, incluso si está vacío
        }
        // Si es otro tipo (Long, Integer, etc.), convertirlo a string
        String result = String.valueOf(value);
        Log.d("UserProfile", "Campo " + key + " no es String, tipo: " + value.getClass().getSimpleName() + ", valor: " + result);
        return result;
    }

    // Helper method para obtener booleans de forma segura
    private static Boolean getBooleanFromMap(Map<String, Object> map, String key, Boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("email", email);
        map.put("displayName", displayName);
        map.put("authMethod", authMethod);
        map.put("fullName", fullName);
        map.put("dateOfBirth", dateOfBirth);
        map.put("gender", gender);
        map.put("curp", curp);
        map.put("ineScanUrl", ineScanUrl);
        map.put("career", career);
        map.put("controlNumber", controlNumber);
        map.put("medicalConditions", medicalConditions);
        map.put("phoneNumber", phoneNumber);
        map.put("emergencyContactName", emergencyContactName);
        map.put("emergencyContactPhone", emergencyContactPhone);
        map.put("profileImageUrl", profileImageUrl);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        map.put("isProfileComplete", isProfileComplete);
        map.put("userType", userType);

        // Nuevos campos añadidos
        map.put("supervisorId", supervisorId);
        map.put("supervisorName", supervisorName);
        map.put("isApproved", isApproved);

        return map;
    }

    // --- MÉTODOS DE LÓGICA ---

    public int getProfileCompleteness() {
        int totalFields = 8;
        int filledFields = 0;
        if (fullName != null && !fullName.isEmpty()) filledFields++;
        if (dateOfBirth != null && !dateOfBirth.isEmpty()) filledFields++;
        if (gender != null && !gender.isEmpty()) filledFields++;
        if (curp != null && !curp.isEmpty()) filledFields++;
        if (career != null && !career.isEmpty()) filledFields++;
        if (controlNumber != null && !controlNumber.isEmpty()) filledFields++;
        if (phoneNumber != null && !phoneNumber.isEmpty()) filledFields++;
        if (emergencyContactName != null && !emergencyContactName.isEmpty()) filledFields++;

        if (totalFields == 0) return 100;
        return (filledFields * 100) / totalFields;
    }

    public int getAge() {
        if (dateOfBirth == null || dateOfBirth.isEmpty()) return 0;
        try {
            String[] parts = dateOfBirth.split("/");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int year = Integer.parseInt(parts[2]);

            Calendar dob = Calendar.getInstance();
            dob.set(year, month, day);
            Calendar today = Calendar.getInstance();

            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return age;
        } catch (Exception e) {
            return 0;
        }
    }

    // --- Getters y Setters ---

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getAuthMethod() { return authMethod; }
    public void setAuthMethod(String authMethod) { this.authMethod = authMethod; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getCurp() { return curp; }
    public void setCurp(String curp) { this.curp = curp; }
    public String getIneScanUrl() { return ineScanUrl; }
    public void setIneScanUrl(String ineScanUrl) { this.ineScanUrl = ineScanUrl; }
    public String getCareer() { return career; }
    public void setCareer(String career) { this.career = career; }
    public String getControlNumber() { return controlNumber; }
    public void setControlNumber(String controlNumber) { this.controlNumber = controlNumber; }
    public String getMedicalConditions() { return medicalConditions; }
    public void setMedicalConditions(String medicalConditions) { this.medicalConditions = medicalConditions; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public boolean isProfileComplete() { return isProfileComplete; }
    public void setProfileComplete(boolean profileComplete) { this.isProfileComplete = profileComplete; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    // Nuevos getters y setters
    public String getSupervisorId() { return supervisorId; }
    public void setSupervisorId(String supervisorId) { this.supervisorId = supervisorId; }
    public String getSupervisorName() { return supervisorName; }
    public void setSupervisorName(String supervisorName) { this.supervisorName = supervisorName; }
    public boolean isApproved() { return isApproved; }
    public void setApproved(boolean approved) { isApproved = approved; }
}