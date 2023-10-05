package com.tech_hub.techhub.service.user;

import com.tech_hub.techhub.dto.UserDto;
//import com.tech_hub.TechHub.entity.PasswordResetToken;
import com.tech_hub.techhub.entity.Cart;
import com.tech_hub.techhub.entity.Role;
import com.tech_hub.techhub.entity.UserEntity;
import com.tech_hub.techhub.exception.PasswordChangeException;
import com.tech_hub.techhub.repository.RoleRepository;
//import com.tech_hub.TechHub.repository.TokenRepository;
import com.tech_hub.techhub.repository.TokenRepository;
import com.tech_hub.techhub.repository.UserRepository;


import com.tech_hub.techhub.util.EmailUtil;
import com.tech_hub.techhub.util.OtpUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Data
public class UserServiceImpl implements UserService{

    @Autowired
    UserRepository userRepository;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder encoder;
    int flag = 0;
    @Autowired
    private OtpUtil otpUtil;
    @Autowired
    private EmailUtil emailUtil;
    @Autowired
    JavaMailSender javaMailSender;
    @Autowired
    TokenRepository tokenRepository;
    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    public boolean verifyAccount(UserDto userDto){
        UserEntity user = userRepository.findByEmail(userDto.getEmail()).orElseThrow(()
                ->new RuntimeException("User Not found with this email"));

        if (user.getOtp().equals(userDto.getOtp()) && Duration.between(user.getOtpGeneratedTime(),
                LocalDateTime.now()).getSeconds()< (30*60)){
            user.setVerified(true);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    @Override
    public Optional<UserEntity> findByEmail(String email) {
       return userRepository.findByEmail(email);
    }

    @Override
    public void deleteCart(Cart cart) {
        UserEntity user = userRepository.findById(cart.getUser().getId()).orElse(null);
        assert user != null;
        user.setCart(null);
        userRepository.save(user);

    }
    @Override
    public Page<UserEntity> findAllPage(int pageNo, int pageSize, String field, String sortDirection) {
        Sort sort = sortDirection.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(field).ascending() :
                Sort.by(field).descending();
        Pageable pageable = PageRequest.of(pageNo-1,pageSize,sort);

        return userRepository.findAll(pageable);
    }
    @Override
    public void changePassword(String username, String currentPassword, String newPass) throws PasswordChangeException {
        Optional<UserEntity> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isPresent()){
            UserEntity user = optionalUser.get();
            if (passwordEncoder.matches(currentPassword,user.getPassword())){
                String newPassword = passwordEncoder.encode(newPass);
                user.setPassword(newPassword);
                userRepository.save(user);
            }else {
                throw new PasswordChangeException("password incorrect");
            }
        }else {
            throw new UsernameNotFoundException("user not found");
        }
    }

    @Override
    public boolean isVerified(String name) {
        Optional<UserEntity> optionalUser = userRepository.findByUsername(name);
        if (optionalUser.isPresent()){
            UserEntity user = optionalUser.get();
            return user.isVerified();
        }
        return false;
    }

    @Override
    public boolean addUser(UserDto userDto) {
        try {
        Optional<UserEntity> userByUserName = userRepository.findByUsername(userDto.getUsername());
        Optional<UserEntity> userByEmail = userRepository.findByEmail(userDto.getEmail());

        if (userByUserName.isPresent() || userByEmail.isPresent()) {
            this.flag = 1;
            return false;
        }
        if (!userDto.getPassword().equals(userDto.getConfirmPassword())){
            return false;
        }
        String otp = otpUtil.generateOtp();

            emailUtil.sentOtp(userDto.getEmail(),otp);

            UserEntity newUser = new UserEntity();
            newUser.setFirstName(userDto.getFirstName());
            newUser.setLastName(userDto.getLastName());
            newUser.setUsername(userDto.getUsername());
            newUser.setEmail(userDto.getEmail());
            newUser.setPhoneNumber(userDto.getPhoneNumber());
            newUser.setPassword(encoder.encode(userDto.getPassword()));
            newUser.setOtp(otp);
            newUser.setOtpGeneratedTime(LocalDateTime.now());
            newUser.setEnabled(true);
            Role role = roleRepository.findByRoleName("ROLE_USER");
            newUser.setRoles(role);
            userRepository.save(newUser);
            return true;
        }catch (MessagingException e){
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void save(UserEntity user) {
        userRepository.save(user);
    }

    @Override
    public List<UserEntity> findAll() {
        return userRepository.findAll();
    }

    @Override
    public Optional<UserEntity> findById(Long id) {
        return userRepository.findById(id);
    }
    @Override
    public void updateUser(Long id, UserEntity updateUser) {
        Optional<UserEntity> getUser = userRepository.findById(id);
        if (getUser.isPresent()) {
            UserEntity theUser = getUser.get();
            theUser.setFirstName(updateUser.getFirstName());
            theUser.setLastName(updateUser.getLastName());
            theUser.setUsername(updateUser.getUsername());
            theUser.setEmail(updateUser.getEmail());
            theUser.setPhoneNumber(updateUser.getPhoneNumber());

        userRepository.save(theUser);

        }else {
            throw new UsernameNotFoundException("user not found");
        }
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    @Override
    public UserEntity findByPhoneNumber(Long phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }
    @Override
    public void blockUser(Long id) {
        UserEntity user = userRepository.findById(id).orElseThrow(()->
                new IllegalArgumentException("User Not Found With Id"+id));
        user.setEnabled(false);
        userRepository.save(user);
    }
    @Override
    public void unBlockUser(Long id) {
        UserEntity user = userRepository.findById(id).orElseThrow(()->
                new IllegalArgumentException("User Not Found With Id"+id));
        user.setEnabled(true);
        userRepository.save(user);
    }










//    @Override
//    public boolean reGenerateOtp(String email) {
//        Optional<UserEntity> optionalUser = userRepository.findByEmail(email);
//        if (optionalUser.isPresent()){
//            UserEntity user = optionalUser.get();
//
//            String otp = otpUtil.generateOtp();
//            try {
//                emailUtil.sentOtp(email, otp);
//            } catch (MessagingException e) {
//                throw new RuntimeException("unable to sent otp please try again");
//            }
//            user.setOtp(otp);
//            user.setOtpGeneratedTime(LocalDateTime.now());
//            userRepository.save(user);
//            return true;
//        }else {
//            return false;
//        }
//    }


    //<----------------------------forgot password field---------------------------->

//    public UserEntity forgotPassword(OtpDto otpDto){
//        Optional<UserEntity> optionalUser = userRepository.findByEmail(otpDto.getEmail());
//        if (optionalUser.isPresent()){
//            UserEntity user = optionalUser.get();
//
//            String otp = otpUtil.generateOtp();
//            try {
//                emailUtil.sentOtp(user.getEmail(), otp);
//            } catch (MessagingException e) {
//                throw new RuntimeException("unable to sent otp please try again");
//            }
//            user.setOtp(otp);
//            user.setOtpGeneratedTime(LocalDateTime.now());
//            userRepository.save(user);
//            return user;
//
//        }else {
//            throw new UsernameNotFoundException("user not found");
//        }
//    }

//public boolean verifyOtpAndResetPassword(String email,String otp,String newPassword){
//        Optional<UserEntity> optionalUser = userRepository.findByEmail(email);
//        if (optionalUser.isPresent()){
//            UserEntity user = optionalUser.get();
//
//            LocalDateTime otpGeneratedTime = user.getOtpGeneratedTime();
//            if (otp.equals(user.getOtp()) && otpGeneratedTime != null &&
//                    otpGeneratedTime.plusMinutes(15).isAfter(LocalDateTime.now())){
//                user.setPassword(newPassword);
//
//                user.setOtp(null);
//                user.setOtpGeneratedTime(null);
//
//                userRepository.save(user);
//
//                return true;
//            }else {
//                return false;
//            }
//        }else {
//            return false;
//        }
//}









//    @Override
//    public String forgotPassword(String email) {
//        UserEntity user = userRepository.findByEmail(email).orElseThrow(
//                () ->new RuntimeException("user not found with email "+email));
//
//        try {
//            emailUtil.sentSetPasswordEmail(email);
//        } catch (MessagingException e) {
//            throw new RuntimeException("unable to set password please try again");
//        }
//
//
//        return "please check your email for set password";
//    }
//
//    @Override
//    public String setPassword(String email, String newPassword) {
//        UserEntity user = userRepository.findByEmail(email).orElseThrow(
//                () ->new RuntimeException("user not found with email "+email));
//
//        user.setPassword(newPassword);
//        userRepository.save(user);
//
//        return "password updated now you can login";
//    }
//
//
//
//    @Override
//    public String sentEmail(UserEntity user) {
//        try {
//            String resetLink = generateResetToken(user);
//
//            SimpleMailMessage message = new SimpleMailMessage();
//            message.setFrom("sreekanthv1995@gmail.com");
//            message.setTo(user.getEmail());
//
//            message.setSubject("Welcome to techHUB");
//            message.setText("Hello \n\n" + "Please click on this link to Reset your Password :" + resetLink + ". \n\n"
//                    + "Regards \n" + "techHUB");
//            javaMailSender.send(message);
//            return "success";
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        return "error";
//    }
//
//    @Override
//    public boolean hasExpired(LocalDateTime expiryDateTime) {
//        LocalDateTime currentDateTime = LocalDateTime.now();
//        return expiryDateTime.isAfter(currentDateTime);
//    }
//
//    private String generateResetToken(UserEntity user) {
//        UUID uuid =UUID.randomUUID();
//        LocalDateTime currentDateTime = LocalDateTime.now();
//        LocalDateTime expiryTimeDate = currentDateTime.plusMinutes(30);
//        PasswordResetToken resetToken = new PasswordResetToken();
//        resetToken.setUser(user);
//        resetToken.setToken(uuid.toString());
//        resetToken.setExpiryDateTime(expiryTimeDate);
////        resetToken.setUser(user);
//        tokenRepository.save(resetToken);
//
//        String endPointUrl = "http://localhost:8080/resetPassword";
//        System.out.println(endPointUrl + "/" + resetToken.getToken());
//        return endPointUrl + "/" + resetToken.getToken();
//    }
}






//    @Override
//    public UserEntity findByPhone(Long phone) {
//        return userRepository.findByPhoneNumber(phone);
//    }
//    @Override
//    public String generateOtpAndSent(UserEntity userData) {
//        Optional<UserEntity> user = userRepository.findByEmail(userData.getEmail());
//        int otp = (int)(Math.random()*9000)+1000;
//        user.setotp(otp);
//        userRepository.save(user);
//        Twilio.init(acc);
//    }
//twilio
//    @Override
//    public void sentOtpAndGenerate(UserEntity user) {
//        OtpEntity otpEntity = otpService.generateOtp(user);
//
//        String messageContent = "Your OTP is " + otpEntity.getOtp();
//        String userPhoneNumber = "7736331483";
//
//        twilioService.sendSms(userPhoneNumber,messageContent);
//
//    }
//    public UserDto register(UserDto userDto){
//        String otp = otpUtil.generateOtp();
//        try {
//            emailUtil.sentOtp(userDto.getEmail(),otp);
//        }catch (MessagingException e){
//            throw new RuntimeException("unable to sent otp try again");
//        }
//        UserEntity user = new UserEntity();
//        user.setFirstName(userDto.getFirstName());
//        user.setLastName(userDto.getLastName());
//        user.setEmail(userDto.getEmail());
//        user.setUsername(userDto.getUsername());
//        user.setPassword(encoder.encode(userDto.getPassword()));
//        user.setOtp(otp);
//        user.setOtpGeneratedTime(LocalDateTime.now());
//        user.setEnabled(true);
//        user.setActive(true);
//
//        userRepository.save(user);
//        return "user registration successful";
//    }

//

