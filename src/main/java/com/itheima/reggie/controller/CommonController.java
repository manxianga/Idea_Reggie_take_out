package com.itheima.reggie.controller;

import com.itheima.reggie.common.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

/**
 * 主要处理文件的上传和下载
 */

@RestController
@RequestMapping("/common")
public class CommonController {

//  将配置文件里面的预设路径赋值给 basePath
    @Value("${reggie.path}")
    private String basePath;

    /**
     * 文件上传
     * @param file 是一个临时文件，需要转存到指定位置，否则本次请求完成后临时文件会被删除
     * @return
     */
    @PostMapping("/upload")
    public R<String> upload(MultipartFile file){

        //原始文件名
        final String originalFilename = file.getOriginalFilename();
        //获取文件名后缀
        final String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        //为避免文件名称上传重复，这里使用uuid随机生成名称+原始后缀
        String fileName = UUID.randomUUID().toString() + suffix;

        //创建一个目录对象
        final File dir = new File(basePath);
        //判断目录是否存在
        if(!dir.exists()){
            //如果目录不存在，则创建
            dir.mkdirs();
        }
        try {
            file.transferTo(new File(basePath + fileName));
            return R.success(fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return R.error("保存失败");
        }
    }

    /**
     * 文件下载
     * @param name
     * @param response
     */
    @GetMapping("/download")
    public void download(String name, HttpServletResponse  response){
        try {
            //输入流，读取文件内容
            final FileInputStream fileInputStream = new FileInputStream(new File(basePath + name));
            //输出流，通过输出流将文件写回浏览器
            ServletOutputStream outputStream = response.getOutputStream();
            //写入文件类型
            response.setContentType("image/jpeg");//固定写法

            int len=0;
            byte[] bytes = new byte[1024];//每次读取1024字节数据
            while ((len = fileInputStream.read(bytes)) != -1){//如果读取的长度不等于-1，文件还没读取完毕
                outputStream.write(bytes,0,len);//写入数据，从0开始，写入len长度
                outputStream.flush();//写入完成，刷新
            }

            //关闭资源
            outputStream.close();
            fileInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
