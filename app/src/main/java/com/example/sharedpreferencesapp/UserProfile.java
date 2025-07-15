package com.example.sharedpreferencesapp;

import java.util.HashMap;
import java.util.Map;

/**
 * Modelo de datos para el perfil de usuario
 *
 * @author anthonyllan
 * @version 1.0
 * @since 2025-07-15
 */
public class UserProfile {

    // Campos obligatorios
    private String userId;
    private String email;
    private String displayName;
    private String authMethod; // "normal", "google"

    // Campos del perfil (opcionales)
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String profileImageUrl;
    private String jobTitle;
    private String department;
    private String institution;
    private String bio;
    private String dateOfBirth;
    private String gender;
    private String address;
    private String city;
    private String state;
    private String country;
    private String zipCode;

    // Metadatos
    private String createdAt;
    private String updatedAt;
    private boolean isActive;
    private boolean isProfileComplete;

    // Constructores
    public UserProfile() {
        // Constructor vacío requerido para Firebase
    }

    public UserProfile(String userId, String email, String displayName, String authMethod) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.authMethod = authMethod;
        this.isActive = true;
        this.isProfileComplete = false;
        this.createdAt = String.valueOf(System.currentTimeMillis());
        this.updatedAt = String.valueOf(System.currentTimeMillis());
    }

    // Getters y Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAuthMethod() { return authMethod; }
    public void setAuthMethod(String authMethod) { this.authMethod = authMethod; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isProfileComplete() { return isProfileComplete; }
    public void setProfileComplete(boolean profileComplete) { isProfileComplete = profileComplete; }

    /**
     * Convierte el perfil a un Map para Firebase
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("email", email);
        map.put("displayName", displayName);
        map.put("authMethod", authMethod);
        map.put("firstName", firstName);
        map.put("lastName", lastName);
        map.put("phoneNumber", phoneNumber);
        map.put("profileImageUrl", profileImageUrl);
        map.put("jobTitle", jobTitle);
        map.put("department", department);
        map.put("institution", institution);
        map.put("bio", bio);
        map.put("dateOfBirth", dateOfBirth);
        map.put("gender", gender);
        map.put("address", address);
        map.put("city", city);
        map.put("state", state);
        map.put("country", country);
        map.put("zipCode", zipCode);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        map.put("isActive", isActive);
        map.put("isProfileComplete", isProfileComplete);
        return map;
    }

    /**
     * Crea un UserProfile desde un Map de Firebase
     */
    public static UserProfile fromMap(Map<String, Object> map) {
        UserProfile profile = new UserProfile();
        profile.setUserId((String) map.get("userId"));
        profile.setEmail((String) map.get("email"));
        profile.setDisplayName((String) map.get("displayName"));
        profile.setAuthMethod((String) map.get("authMethod"));
        profile.setFirstName((String) map.get("firstName"));
        profile.setLastName((String) map.get("lastName"));
        profile.setPhoneNumber((String) map.get("phoneNumber"));
        profile.setProfileImageUrl((String) map.get("profileImageUrl"));
        profile.setJobTitle((String) map.get("jobTitle"));
        profile.setDepartment((String) map.get("department"));
        profile.setInstitution((String) map.get("institution"));
        profile.setBio((String) map.get("bio"));
        profile.setDateOfBirth((String) map.get("dateOfBirth"));
        profile.setGender((String) map.get("gender"));
        profile.setAddress((String) map.get("address"));
        profile.setCity((String) map.get("city"));
        profile.setState((String) map.get("state"));
        profile.setCountry((String) map.get("country"));
        profile.setZipCode((String) map.get("zipCode"));
        profile.setCreatedAt((String) map.get("createdAt"));
        profile.setUpdatedAt((String) map.get("updatedAt"));
        profile.setActive(Boolean.TRUE.equals(map.get("isActive")));
        profile.setProfileComplete(Boolean.TRUE.equals(map.get("isProfileComplete")));
        return profile;
    }

    /**
     * Obtiene el nombre completo del usuario
     */
    public String getFullName() {
        if (firstName != null && lastName != null && !firstName.isEmpty() && !lastName.isEmpty()) {
            return firstName + " " + lastName;
        } else if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        } else {
            return email != null ? email : "Usuario";
        }
    }

    /**
     * Calcula el porcentaje de completitud del perfil
     */
    public int getProfileCompleteness() {
        int totalFields = 15; // Campos principales del perfil
        int completedFields = 0;

        if (firstName != null && !firstName.isEmpty()) completedFields++;
        if (lastName != null && !lastName.isEmpty()) completedFields++;
        if (phoneNumber != null && !phoneNumber.isEmpty()) completedFields++;
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) completedFields++;
        if (jobTitle != null && !jobTitle.isEmpty()) completedFields++;
        if (department != null && !department.isEmpty()) completedFields++;
        if (institution != null && !institution.isEmpty()) completedFields++;
        if (bio != null && !bio.isEmpty()) completedFields++;
        if (dateOfBirth != null && !dateOfBirth.isEmpty()) completedFields++;
        if (gender != null && !gender.isEmpty()) completedFields++;
        if (address != null && !address.isEmpty()) completedFields++;
        if (city != null && !city.isEmpty()) completedFields++;
        if (state != null && !state.isEmpty()) completedFields++;
        if (country != null && !country.isEmpty()) completedFields++;
        if (zipCode != null && !zipCode.isEmpty()) completedFields++;

        return (completedFields * 100) / totalFields;
    }
}