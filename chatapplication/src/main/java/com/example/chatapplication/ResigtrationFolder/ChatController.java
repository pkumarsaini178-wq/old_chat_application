package com.example.chatapplication.ResigtrationFolder;

import org.springframework.beans.factory.annotation.Autowired;
import com.example.chatapplication.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import com.example.chatapplication.SecurityConfigration.JwtUntil;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class ChatController {

    @Autowired
    private Chatservicefile chatService;

    @Autowired
    private com.example.chatapplication.MediaService mediaService;

    @Autowired
    private JwtUntil jwtUntil;

    @Autowired
    private com.example.chatapplication.TokenBlacklistService tokenBlacklistService;

    @Autowired
    private EmailService emailService;

    @Autowired
    ChatSinginRepo chatsinginRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${admin.emails:}")
    private String adminEmails;

    @GetMapping("/")
    public RedirectView redi(HttpServletRequest request) {
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token != null && jwtUntil.Token_is_vailid(token)) {
            // Token is valid, user is already logged in, redirect to home page
            return new RedirectView("/homepage.html");
        }
        // Token is missing or invalid, redirect to login page
        return new RedirectView("/login.html");
    }

    @PostMapping("/loginpage")
    public RedirectView loginString(
            @RequestParam(value = "user_email", required = false) String user_email,
            @RequestParam String passowrd,
            HttpServletResponse response) {
        String inputEmail = user_email.trim();
        if (inputEmail != null && passowrd != null) {
            try {
                ChatSingin chatuser = chatService.loginUser(inputEmail, passowrd);
                if (chatuser != null) {
                    // Generate JWT token based on user email
                    String emailForToken = chatuser.getUseremail();
                    String token = jwtUntil.gunrateToken(emailForToken);

                    // Create cookie to store the JWT token for 1 week
                    Cookie cookie = new Cookie("jwt", token);
                    cookie.setHttpOnly(true); // secure against XSS
                    cookie.setSecure(true);   // only sent over HTTPS
                    cookie.setPath("/");      // available across the entire application
                    cookie.setMaxAge(7 * 24 * 60 * 60); // 1 week expiry in seconds
                    // Set SameSite=None via Set-Cookie header for cross-origin support
                    String setCookieHeader = cookie.getName() + "=" + cookie.getValue()
                            + "; Path=" + cookie.getPath()
                            + "; Max-Age=" + cookie.getMaxAge()
                            + "; HttpOnly; Secure; SameSite=None";
                    response.addHeader("Set-Cookie", setCookieHeader);

                    return new RedirectView("/homepage.html");
                }
            } catch (RuntimeException e) {
                if ("ACCOUNT_BLOCKED".equals(e.getMessage())) {
                    return new RedirectView("/login.html?error=" + "Your account has been blocked by the admin.");
                }
            }
        }
        return new RedirectView("/login.html?error=" + "Email or password is not valid");
    }

    @PostMapping("/siginpage")
    @ResponseBody
    public java.util.Map<String, String> singinString(
            @RequestParam(value = "user_name", required = false) String user_name,
            @RequestParam(value = "user_email", required = false) String user_email,
            @RequestParam(value = "passowrd", required = false) String passowrd,
            @RequestParam(value = "current_password", required = false) String current_password) {
        java.util.Map<String, String> result = new java.util.HashMap<>();
        
        if (user_email == null || user_name == null || passowrd == null || current_password == null ||
            user_email.trim().isEmpty() || user_name.trim().isEmpty() || passowrd.trim().isEmpty()) {
            result.put("status", "ERROR");
            result.put("message", "All fields are required");
            return result;
        }

        String cleanEmail = user_email.trim().toLowerCase();
        String cleanName = user_name.trim();
        String cleanPass = passowrd.trim();
        String cleanConfirm = current_password.trim();

        if (!cleanPass.equals(cleanConfirm)) {
            result.put("status", "ERROR");
            result.put("message", "Passwords do not match");
            return result;
        }

        try {
            ChatSingin datasave = new ChatSingin();
            datasave.setUsername(cleanName);
            datasave.setUseremail(cleanEmail);
            datasave.setPassword(passwordEncoder.encode(cleanPass));
            datasave.setCurrentpassword(passwordEncoder.encode(cleanConfirm));
            chatService.registerUser(datasave);
            
            result.put("status", "SUCCESS");
            result.put("message", "Account created successfully! Please sign in.");
            result.put("redirect", "/login.html");
            return result;
        } catch (Exception e) {
            String errorDetails = (e.getMessage() != null && !e.getMessage().isEmpty()) 
                    ? e.getMessage() 
                    : ("Registration failed (" + e.getClass().getSimpleName() + ")");
            result.put("status", "ERROR");
            result.put("message", errorDetails);
            return result;
        }
    }

    private static class OtpEntry {
        String otp;
        LocalDateTime createdAt;
        OtpEntry(String otp) { this.otp = otp; this.createdAt = LocalDateTime.now(); }
        boolean isExpired() { return LocalDateTime.now().isAfter(createdAt.plusMinutes(10)); }
    }
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    @GetMapping("/forgetpassword")
    public RedirectView forgetpasswordPage() {
        return new RedirectView("forgetpassword.html");
    }

    @PostMapping("/send-otp")
    public RedirectView sendOtp(@RequestParam String email) {
        if (email == null || email.isEmpty()) {
            return new RedirectView("forgetpassword.html?error=" + "Email is required");
        }
        Optional<ChatSingin> checkemail = chatsinginRepo.findByuseremail(email);
        if (checkemail.isPresent()) {
            SecureRandom secureRandom = new SecureRandom();
            String otp = String.valueOf(100000 + secureRandom.nextInt(900000));
            otpStore.put(email, new OtpEntry(otp));

            try {
                emailService.sendOtp(email, otp);
                return new RedirectView("verify-otp.html?email=" + email);
            } catch (Exception e) {
                return new RedirectView("forgetpassword.html?error=" + "Failed to send email. Try again.");
            }
        } else {
            return new RedirectView("forgetpassword.html?error=" + "Email not found");
        }
    }

    @PostMapping("/verify-otp")
    public RedirectView verifyOtp(@RequestParam String email, @RequestParam String otp) {
        OtpEntry entry = otpStore.get(email);
        if (entry == null) {
            return new RedirectView("verify-otp.html?email=" + email + "&error=" + "No OTP found. Request a new one.");
        }
        if (entry.isExpired()) {
            otpStore.remove(email);
            return new RedirectView("verify-otp.html?email=" + email + "&error=" + "OTP has expired. Request a new one.");
        }
        if (entry.otp.equals(otp)) {
            otpStore.remove(email);
            return new RedirectView("setpassword.html?email=" + email);
        } else {
            return new RedirectView("verify-otp.html?email=" + email + "&error=" + "Invalid OTP. Try again.");
        }
    }

    @GetMapping("/setpassword")
    public RedirectView resetpass(@RequestParam String email) {
        return new RedirectView("setpassword.html?email=" + email);
    }

    @PostMapping("/setpassword")
    public RedirectView resetpasswordString(@RequestParam String email, @RequestParam String password,
            @RequestParam String currentpassword) {
        if (!password.equals(currentpassword)) {
            return new RedirectView("setpassword.html?email=" + email + "&error=" + "Passwords do not match");
        }
        Optional<ChatSingin> checkemail = chatsinginRepo.findByuseremail(email);
        if (checkemail.isPresent()) {
            ChatSingin chatuser = checkemail.get();
            chatuser.setPassword(passwordEncoder.encode(password));
            chatuser.setCurrentpassword(passwordEncoder.encode(currentpassword));
            chatService.registerUser(chatuser);
            return new RedirectView("login.html?success=" + "Password reset successful. Sign in now.");
        } else {
            return new RedirectView("setpassword.html?email=" + email + "&error=" + "Email not valid");
        }
    }

    @GetMapping("/active-users")
    @ResponseBody
    public List<ChatSingin> showthe_all_activeMembers() {
        List<ChatSingin> users = chatService.getAllUsers();
        for (ChatSingin user : users) {
            user.setPassword(null);
            user.setCurrentpassword(null);
        }
        return users;
    }

    @GetMapping("/api/users/search")
    @ResponseBody
    public List<ChatSingin> searchUsers(@RequestParam String query) {
        List<ChatSingin> allUsers = chatService.getAllUsers();
        List<ChatSingin> result = new java.util.ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (ChatSingin user : allUsers) {
            String name = user.getUsername() != null ? user.getUsername().toLowerCase() : "";
            String email = user.getUseremail() != null ? user.getUseremail().toLowerCase() : "";
            if (name.contains(lowerQuery) || email.contains(lowerQuery)) {
                user.setPassword(null);
                user.setCurrentpassword(null);
                result.add(user);
            }
        }
        return result;
    }

    @GetMapping("/api/current-user")
    @ResponseBody
    public java.util.Map<String, String> getCurrentUser() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        java.util.Map<String, String> map = new java.util.HashMap<>();
        if (auth != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
            map.put("email", auth.getName());
            Optional<ChatSingin> user = chatsinginRepo.findByuseremail(auth.getName());
            if (user.isPresent()) {
                map.put("username", user.get().getUsername());
            }
        }
        return map;
    }

    @GetMapping("/api/token")
    @ResponseBody
    public java.util.Map<String, String> getJwtToken(HttpServletRequest request) {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    map.put("token", cookie.getValue());
                    break;
                }
            }
        }
        return map;
    }

    @PostMapping("/api/user/update-profile")
    @ResponseBody
    public java.util.Map<String, String> updateProfile(@RequestParam String username) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        java.util.Map<String, String> response = new java.util.HashMap<>();
        if (auth != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
            String email = auth.getName();
            Optional<ChatSingin> userOpt = chatsinginRepo.findByuseremail(email);
            if (userOpt.isPresent()) {
                ChatSingin user = userOpt.get();
                user.setUsername(username);
                chatsinginRepo.save(user);
                response.put("status", "SUCCESS");
                response.put("message", "Profile updated successfully!");
                response.put("username", username);
            } else {
                response.put("status", "ERROR");
                response.put("message", "User not found.");
            }
        } else {
            response.put("status", "ERROR");
            response.put("message", "Unauthorized.");
        }
        return response;
    }

    @PostMapping("/api/request/send")
    @ResponseBody
    public com.example.chatapplication.Notification sendRequest(@RequestParam String receiverEmail) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String senderEmail = auth.getName();
        return chatService.sendRequest(senderEmail, receiverEmail);
    }

    @GetMapping("/api/request/pending")
    @ResponseBody
    public List<com.example.chatapplication.Notification> getPendingRequests() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String receiverEmail = auth.getName();
        return chatService.getPendingNotifications(receiverEmail);
    }

    @PostMapping("/api/request/accept")
    @ResponseBody
    public String acceptRequest(@RequestParam Long notificationId) {
        return chatService.acceptRequest(notificationId);
    }

    @PostMapping("/api/request/reject")
    @ResponseBody
    public String rejectRequest(@RequestParam Long notificationId) {
        return chatService.rejectRequest(notificationId);
    }

    @GetMapping("/api/friends")
    @ResponseBody
    public List<com.example.chatapplication.ChatConnection> getFriends() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = auth.getName();
        return chatService.getFriends(currentEmail);
    }

    @Autowired
    private com.example.chatapplication.UserStatusRepo userStatusRepo;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @GetMapping("/api/chat/history")
    @ResponseBody
    public List<com.example.chatapplication.ChatMessageDto> getChatHistory(@RequestParam Long connectionId, @RequestParam(defaultValue = "0") int page) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUser = auth.getName();
        return chatService.getChatHistory(connectionId, currentUser, page);
    }

    @PostMapping("/api/chat/media/upload")
    @ResponseBody
    public java.util.Map<String, Object> uploadMedia(@RequestParam(value = "file") org.springframework.web.multipart.MultipartFile file, @RequestParam Long connectionId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty.");
        }
        
        String mediaContentType = file.getContentType();
        if (mediaContentType == null || (!mediaContentType.startsWith("image/") && !mediaContentType.startsWith("audio/") && !mediaContentType.startsWith("video/"))) {
            throw new RuntimeException("Invalid file type. Only images, audio, and video are allowed.");
        }
        
        Long mediaId = mediaService.uploadMedia(file, connectionId);
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("mediaId", mediaId);
        return response;
    }

    @Autowired
    private com.example.chatapplication.EncryptionService encryptionService;

    @GetMapping("/api/chat/media/{mediaId}")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> getChatMedia(@PathVariable Long mediaId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUser = auth.getName();
        
        com.example.chatapplication.MediaStorageEntity entity = mediaService.getMediaEntity(mediaId);
        if (entity == null || entity.getMediaData() == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        
        // Authorization check: Verify user is part of the connection
        List<com.example.chatapplication.ChatConnection> friends = chatService.getFriends(currentUser);
        boolean hasAccess = friends.stream().anyMatch(c -> c.getId().equals(entity.getConnectionId()));
        if (!hasAccess) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }
        
        try {
            byte[] decryptedMedia = encryptionService.decrypt(entity.getMediaData(), entity.getMediaEncryptionIv());
            org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(decryptedMedia);
            return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(entity.getMediaContentType()))
                .cacheControl(org.springframework.http.CacheControl.maxAge(7, java.util.concurrent.TimeUnit.DAYS))
                .body(resource);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/api/chat/clear/{connectionId}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<String> clearChat(@PathVariable Long connectionId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUser = auth.getName();

        List<com.example.chatapplication.ChatConnection> friends = chatService.getFriends(currentUser);
        boolean hasAccess = friends.stream().anyMatch(c -> c.getId().equals(connectionId));
        if (!hasAccess) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body("Unauthorized");
        }

        chatService.clearChatHistory(connectionId, currentUser);
        return org.springframework.http.ResponseEntity.ok("SUCCESS");
    }

    @PostMapping("/api/chat/delete")
    @ResponseBody
    public org.springframework.http.ResponseEntity<String> deleteChat(@RequestParam Long connectionId, @RequestParam String timeframe) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUser = auth.getName();
        
        List<com.example.chatapplication.ChatConnection> friends = chatService.getFriends(currentUser);
        boolean hasAccess = friends.stream().anyMatch(c -> c.getId().equals(connectionId));
        if (!hasAccess) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body("Unauthorized to delete this chat");
        }
        
        try {
            chatService.deleteChatHistory(connectionId, timeframe);
            return org.springframework.http.ResponseEntity.ok("SUCCESS");
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete chat");
        }
    }

    @GetMapping("/api/user/status/{userEmail}")
    @ResponseBody
    public java.util.Map<String, Object> getUserStatus(@PathVariable String userEmail) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        java.util.Optional<com.example.chatapplication.UserStatus> status = userStatusRepo.findById(userEmail);
        if (status.isPresent()) {
            result.put("isOnline", status.get().getIsOnline());
            result.put("lastSeen", status.get().getLastSeen() != null
                    ? status.get().getLastSeen().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    : null);
        } else {
            result.put("isOnline", false);
            result.put("lastSeen", null);
        }
        return result;
    }

    @PostMapping("/api/user/online")
    @ResponseBody
    public java.util.Map<String, String> setUserOnline() {
        String userEmail = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        java.util.Map<String, String> result = new java.util.HashMap<>();

        java.util.Optional<com.example.chatapplication.UserStatus> existing = userStatusRepo.findById(userEmail);
        boolean wasOffline = true;

        if (existing.isPresent()) {
            com.example.chatapplication.UserStatus status = existing.get();
            wasOffline = !Boolean.TRUE.equals(status.getIsOnline());
            if (wasOffline) {
                status.setIsOnline(true);
                userStatusRepo.save(status);
            }
        } else {
            com.example.chatapplication.UserStatus status = new com.example.chatapplication.UserStatus();
            status.setUserEmail(userEmail);
            status.setIsOnline(true);
            status.setActiveConnections(0);
            userStatusRepo.save(status);
        }

        if (wasOffline) {
            java.util.Map<String, Object> statusMsg = new java.util.HashMap<>();
            statusMsg.put("email", userEmail);
            statusMsg.put("online", true);
            statusMsg.put("lastSeen", null);
            messagingTemplate.convertAndSend("/topic/status", (Object) statusMsg);
        }

        result.put("status", "online");
        return result;
    }

    @PostMapping("/logout")
    public org.springframework.http.ResponseEntity<Void> logoutUser(HttpServletRequest request, HttpServletResponse response) {
        // Blacklist the active JWT token on logout
        try {
            String token = null;
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("jwt".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
            if (token != null && jwtUntil.Token_is_vailid(token)) {
                java.util.Date expDate = jwtUntil.getTokenExpiration(token);
                java.time.LocalDateTime expiry = expDate.toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime();
                tokenBlacklistService.blacklistToken(token, expiry);
            }
        } catch (Exception e) {
            // Ignore token blacklisting errors during logout
        }

        // Mark user offline before clearing cookie
        try {
            String userEmail = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            if (userEmail != null && !"anonymousUser".equals(userEmail)) {
                java.util.Optional<com.example.chatapplication.UserStatus> existing = userStatusRepo.findById(userEmail);
                if (existing.isPresent()) {
                    com.example.chatapplication.UserStatus status = existing.get();
                    status.setIsOnline(false);
                    status.setLastSeen(LocalDateTime.now());
                    userStatusRepo.save(status);
                    java.util.Map<String, Object> statusMsg = new java.util.HashMap<>();
                    statusMsg.put("email", userEmail);
                    statusMsg.put("online", false);
                    statusMsg.put("lastSeen", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    messagingTemplate.convertAndSend("/topic/status", (Object) statusMsg);
                }
            }
        } catch (Exception e) {
            // Ignore errors during status update on logout
        }
        String clearCookieHeader = "jwt=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None";
        response.addHeader("Set-Cookie", clearCookieHeader);
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @PostMapping("/api/user/offline")
    @ResponseBody
    public java.util.Map<String, String> setUserOffline() {
        String userEmail = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        java.util.Map<String, String> result = new java.util.HashMap<>();
        if (userEmail != null && !"anonymousUser".equals(userEmail)) {
            java.util.Optional<com.example.chatapplication.UserStatus> existing = userStatusRepo.findById(userEmail);
            if (existing.isPresent()) {
                com.example.chatapplication.UserStatus status = existing.get();
                status.setIsOnline(false);
                status.setLastSeen(LocalDateTime.now());
                userStatusRepo.save(status);
            }
            result.put("status", "offline");
        }
        return result;
    }

    @GetMapping("/api/user/status/batch")
    @ResponseBody
    public java.util.Map<String, Object> getBatchUserStatus(@RequestParam(required = false) List<String> emails) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        if (emails != null) {
            for (String email : emails) {
                if (email == null || email.trim().isEmpty()) continue;
                java.util.Optional<com.example.chatapplication.UserStatus> status = userStatusRepo.findById(email);
                java.util.Map<String, Object> userStatus = new java.util.HashMap<>();
                if (status.isPresent()) {
                    userStatus.put("isOnline", status.get().getIsOnline());
                    userStatus.put("lastSeen", status.get().getLastSeen() != null
                            ? status.get().getLastSeen().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            : null);
                } else {
                    userStatus.put("isOnline", false);
                    userStatus.put("lastSeen", null);
                }
                result.put(email, userStatus);
            }
        }
        return result;
    }

    @PostMapping("/api/chat/message/delete")
    @ResponseBody
    public org.springframework.http.ResponseEntity<String> deleteMessage(
            @RequestParam Long messageId, 
            @RequestParam String deleteType) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUser = auth.getName();
        
        try {
            boolean success = chatService.deleteMessage(messageId, currentUser, deleteType);
            if (success) {
                return org.springframework.http.ResponseEntity.ok("SUCCESS");
            } else {
                return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body("Unauthorized");
            }
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }

    @PostMapping("/api/chat/message/edit")
    @ResponseBody
    public org.springframework.http.ResponseEntity<String> editMessage(
            @RequestParam Long messageId, 
            @RequestParam String newContent) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUser = auth.getName();
        
        try {
            boolean success = chatService.editMessage(messageId, currentUser, newContent);
            if (success) {
                return org.springframework.http.ResponseEntity.ok("SUCCESS");
            } else {
                return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body("Unauthorized");
            }
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }

    private boolean isAdminEmail(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        String cleanEmail = email.trim().toLowerCase();
        if (adminEmails == null || adminEmails.trim().isEmpty()) return false;
        for (String adminEmail : adminEmails.split(",")) {
            if (adminEmail.trim().toLowerCase().equals(cleanEmail)) {
                return true;
            }
        }
        return false;
    }

    @GetMapping("/api/admin/check")
    @ResponseBody
    public java.util.Map<String, Object> checkAdminStatus() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        String email = (auth != null && auth.getName() != null) ? auth.getName() : "";
        boolean isAdmin = isAdminEmail(email);
        map.put("isAdmin", isAdmin);
        map.put("email", email);
        return map;
    }

    @GetMapping("/api/admin/users")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> getAllUsersForAdmin() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUser = (auth != null && auth.getName() != null) ? auth.getName() : "";
        if (!isAdminEmail(currentUser)) {
            java.util.Map<String, String> res = new java.util.HashMap<>();
            res.put("status", "ERROR");
            res.put("message", "Unauthorized. Only admins can access this.");
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(res);
        }
        List<ChatSingin> users = chatService.getAllUsers();
        for (ChatSingin user : users) {
            user.setPassword(null);
            user.setCurrentpassword(null);
            if (isAdminEmail(user.getUseremail())) {
                user.setRole("ADMIN");
            } else if (user.getRole() == null) {
                user.setRole("USER");
            }
        }
        return org.springframework.http.ResponseEntity.ok(users);
    }

    @PostMapping("/api/admin/block-user")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, String>> blockUserByAdmin(
            @RequestParam String email,
            @RequestParam(defaultValue = "365") int days) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUser = (auth != null && auth.getName() != null) ? auth.getName() : "";
        java.util.Map<String, String> response = new java.util.HashMap<>();

        if (!isAdminEmail(currentUser)) {
            response.put("status", "ERROR");
            response.put("message", "Unauthorized access.");
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(response);
        }

        if (isAdminEmail(email)) {
            response.put("status", "ERROR");
            response.put("message", "Cannot block an admin account.");
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(response);
        }

        boolean success = chatService.blockUserByEmail(email, days);
        if (success) {
            response.put("status", "SUCCESS");
            response.put("message", "User " + email + " has been blocked for 1 year.");
            return org.springframework.http.ResponseEntity.ok(response);
        } else {
            response.put("status", "ERROR");
            response.put("message", "User not found.");
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PostMapping("/api/admin/unblock-user")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, String>> unblockUserByAdmin(@RequestParam String email) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUser = (auth != null && auth.getName() != null) ? auth.getName() : "";
        java.util.Map<String, String> response = new java.util.HashMap<>();

        if (!isAdminEmail(currentUser)) {
            response.put("status", "ERROR");
            response.put("message", "Unauthorized access.");
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(response);
        }

        boolean success = chatService.unblockUserByEmail(email);
        if (success) {
            response.put("status", "SUCCESS");
            response.put("message", "User " + email + " was successfully unblocked.");
            return org.springframework.http.ResponseEntity.ok(response);
        } else {
            response.put("status", "ERROR");
            response.put("message", "User not found.");
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PostMapping("/api/admin/delete-user")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, String>> deleteUserByAdmin(@RequestParam String email) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String currentUser = (auth != null && auth.getName() != null) ? auth.getName() : "";
        
        java.util.Map<String, String> response = new java.util.HashMap<>();
        
        if (!isAdminEmail(currentUser)) {
            response.put("status", "ERROR");
            response.put("message", "Unauthorized access.");
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(response);
        }
        
        if (isAdminEmail(email)) {
            response.put("status", "ERROR");
            response.put("message", "Cannot delete an admin account.");
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(response);
        }
        
        boolean success = chatService.deleteUserByEmail(email);
        if (success) {
            response.put("status", "SUCCESS");
            response.put("message", "User " + email + " account was permanently deleted.");
            return org.springframework.http.ResponseEntity.ok(response);
        } else {
            response.put("status", "ERROR");
            response.put("message", "User not found.");
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(response);
        }
    }
}