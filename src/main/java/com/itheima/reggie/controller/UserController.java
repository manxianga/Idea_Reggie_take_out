package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.R;
import com.itheima.reggie.component.SendEmail;
import com.itheima.reggie.entity.User;
import com.itheima.reggie.service.UserService;
import com.itheima.reggie.utils.SMSUtils;
import com.itheima.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private SendEmail sendEmail;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 发送手机短信验证码
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user,HttpSession session){
//        发送邮箱验证
        val emal = user.getPhone();
        if(emal != null){
            //生成随机的短信验证码
            final String code = ValidateCodeUtils.generateValidateCode(4).toString();
            //将生成的验证码存到Redis，并且设置有效期为5分钟
            redisTemplate.opsForValue().set(emal,code,5, TimeUnit.MINUTES);
            //发送邮箱验证
            sendEmail.sendEmail(emal,code);
            log.info("code: {}",code);

            return R.success("发送成功");
        }
        else {
            return R.error("邮箱为空");
        }

//        发送短信验证
        //获取手机号
//        String phone = user.getPhone();
//        if(phone != null){
//            //生成随机的短信验证码
//            final String code = ValidateCodeUtils.generateValidateCode(4).toString();
//            //调用阿里云提供的短信服务API完成发送短信
//            SMSUtils.sendMessage("阿里云短信测试","SMS_154950909",phone,code);
//            log.info("code: {}",code);
//            return R.success("手机验证码发送成功");
//        }
//        else{
//            return R.success("手机号为空");
//        }
}

    /**
     * 移动端用户登陆
     * @param map
     * @param session
     * @return
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map,HttpSession session){
        //获取手机号
        final String phone = map.get("phone").toString();
        //获取验证码
        final String code = map.get("code").toString();
        //从Redis中获取缓存的验证码
        String codeInSession = redisTemplate.opsForValue().get(phone).toString();
        //进行验证码对比
        if(codeInSession != null && codeInSession.equals(code)){
            //如果能够比对成功，说明登陆成功
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone,phone);

            User user = userService.getOne(queryWrapper);
            if(user == null){
                //判断当前手机号对应的用户是否为新用户，如果是新用户就自动完成注册
                user = new User();
                user.setPhone(phone);
                userService.save(user);
            }
            session.setAttribute("user",user.getId());
            //如果用户登陆成功，则删除Redis中缓存的验证码
            redisTemplate.delete(phone);

            return R.success(user);
        }
        return R.error("登陆失败");
    }

    @PostMapping("/loginout")
    public R<String> loginout(HttpServletRequest request){
        //清理Session中保存的当前员工的id
        request.getSession().removeAttribute("user");
        return R.success("退出成功");
    }
}
