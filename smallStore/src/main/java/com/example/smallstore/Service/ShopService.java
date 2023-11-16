package com.example.smallstore.Service;

import com.example.smallstore.Dto.Shop.ShopNumberCheckRequest;
import com.example.smallstore.Dto.Shop.ShopRegisterRequest;
import com.example.smallstore.Entity.Category;
import com.example.smallstore.Entity.Shop;
import com.example.smallstore.Entity.User;
import com.example.smallstore.Repository.CategoryRepository;
import com.example.smallstore.Repository.ShopRepository;
import com.example.smallstore.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import okhttp3.MediaType;
import org.json.simple.JSONArray;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.json.simple.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ShopService {
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;
    private final S3Serivce s3Serivce;

    // 사업자 등록 번호 확인
    public ResponseEntity numberCheck(ShopNumberCheckRequest shopNumberCheckRequest) {
        String result;
        String api = "https://api.odcloud.kr/api/nts-businessman/v1/status?";
        String serviceKey = "serviceKey=OiJ%2FvDJet5YWO8x6EWtaeg45v4TwLoG%2BLc3CNPvJkbfIeXKrPGnF%2Bk3Gp3ua9Bor4eMgk8JEa9tj%2BVLCcdMlDg%3D%3D" ;
        String apiUrl = api+serviceKey;
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        MediaType mediaType = MediaType.parse("application/json");
        JSONObject requestBody=new JSONObject();
        requestBody.put("b_no",shopNumberCheckRequest.getB_no());
        RequestBody body = RequestBody.create(requestBody.toString(),mediaType);
        Request request = new Request.Builder()
                .url(apiUrl)
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .build();
        //결과 탐색
        try {
            Response response = client.newCall(request).execute();
            JSONArray data = (JSONArray) UtilService.stringToJson(response.body().string()).get("data");
            JSONObject data2 = (JSONObject) data.get(0);
            result = (String) data2.get("b_stt_cd");
            if(result.equals("01")) {
                return ResponseEntity.ok("사업자입니다!");
            }
            return ResponseEntity.badRequest().body("사용할 수 없는 사업자등록번호입니다.");
        } catch (Exception e) {
            UtilService.ExceptionValue(e.getMessage(), String.class);
            return null;
        }
    }

    // 가게 등록
    public ResponseEntity regeist(ShopRegisterRequest shopRegisterRequest) {
        User user = userRepository.findById(shopRegisterRequest.getId()).orElseThrow();
        Category category = categoryRepository.findByNum(shopRegisterRequest.getCategoryNum()).orElseThrow();
        Shop shop = new Shop(shopRegisterRequest, user, category);
        shopRepository.save(shop);
        return ResponseEntity.ok("가게 등록이 완료되었습니다.");
    }

    // 조건별로 가게 보여주기

    // 검색

    // 로고 이미지 저장하기
    public ResponseEntity saveLogoImg(MultipartFile fileName, HttpServletRequest request){
        if(!s3Serivce.uploadImage(fileName)){
            return ResponseEntity.badRequest().body("로고 이미지 저장하지 못했습니다.");
        }
        User user = userService.findUserByToken(request);
        Shop shop = shopRepository.findByUser(user).orElseThrow();
        // db에 로고 파일 이름 저장
        shop.setShopLogo(fileName.getOriginalFilename());

        return ResponseEntity.ok("로고 이미지 저장했습니다.");
    }

    // 가게 이미지 저장하기
    public ResponseEntity saveShopImg(MultipartFile fileName, HttpServletRequest request){
        if(!s3Serivce.uploadImage(fileName)){
            return ResponseEntity.badRequest().body("가게 이미지 저장하지 못했습니다.");
        }
        User user = userService.findUserByToken(request);
        Shop shop = shopRepository.findByUser(user).orElseThrow();
        // db에 로고 파일 이름 저장
        shop.setShopPicture(fileName.getOriginalFilename());

        return ResponseEntity.ok("가게 이미지 저장했습니다.");
    }

    // 이미지 가져오기
    public ResponseEntity downloadLogoImg(String originalFilename){
        //String imgUrl = s3Serivce.downloadImage(originalFilename);

        //return ResponseEntity.ok(imgUrl);
        return ResponseEntity.ok(originalFilename);
    }

    // 로고 이미지 삭제하기
    public ResponseEntity deleteLogoImage(String originalFilename, HttpServletRequest request){
        //s3Serivce.deleteImage(originalFilename);
        User user = userService.findUserByToken(request);
        Shop shop = shopRepository.findByUser(user).orElseThrow();
        shop.setShopLogo("");

        return ResponseEntity.ok("삭제 완료했습니다.");
    }

    // 가게 이미지 삭제하기
    public ResponseEntity deleteShopImage(String originalFilename, HttpServletRequest request){
        //s3Serivce.deleteImage(originalFilename);
        User user = userService.findUserByToken(request);
        Shop shop = shopRepository.findByUser(user).orElseThrow();
        shop.setShopPicture("");

        return ResponseEntity.ok("삭제 완료했습니다.");
    }

}
