package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.AddressBook;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;
    @Autowired
    private SetmealDishService setmealDishService;
    @Autowired
    private CategoryService categoryService;

    /**
     * 新增套餐
     * @param setmealDto
     */
    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto){
        setmealService.saveWithDish(setmealDto);

        return R.success("新增套餐成功");
    }

    /**
     * 套餐分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){
        //分页构造对象
        Page<Setmeal> pageInfo = new Page<>();
        Page<SetmealDto> dtoPage = new Page<>();
        //添加查询条件
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper();
        queryWrapper.like(name != null,Setmeal::getName,name);//根据名字模糊查找
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);//根据更新时间降序

        setmealService.page(pageInfo,queryWrapper);
        BeanUtils.copyProperties(pageInfo,dtoPage,"records");

        final List<SetmealDto> collect = pageInfo.getRecords().stream().map((item) -> {
            SetmealDto setmealDto = new SetmealDto();
            //对象copy
            BeanUtils.copyProperties(item, setmealDto);

            final Category category = categoryService.getById(setmealDto.getCategoryId());
            if(category != null)
                setmealDto.setCategoryName(category.getName());
            return setmealDto;
        }).collect(Collectors.toList());
        dtoPage.setRecords(collect);
        return R.success(dtoPage);
    }

    /**
     * 删除套餐
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){

        setmealService.removeWithDish(ids);
        return R.success("套餐数据删除成功");
    }

    @PostMapping("status/{status}")
    public R<String> status(@PathVariable int status,Long[] ids){
        for (Long id : ids) {
            final Setmeal setmeal = setmealService.getById(id);
            setmeal.setStatus(status);
            setmealService.updateById(setmeal);
        }
        return R.success("修改成功");
    }

    @GetMapping("/{id}")
    public R<SetmealDto> getById(@PathVariable Long id){
        final Setmeal setmeal = setmealService.getById(id);
        final SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal,setmealDto);

        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId,setmeal.getId());
        queryWrapper.orderByDesc(SetmealDish::getUpdateTime);
        final List<SetmealDish> list = setmealDishService.list(queryWrapper);
        setmealDto.setSetmealDishes(list);
        return R.success(setmealDto);
    }

    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto){
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId,setmealDto.getId());
        setmealDishService.remove(queryWrapper);

        final List<SetmealDish> list = setmealDto.getSetmealDishes().stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());


        setmealService.updateById(setmealDto);
        setmealDishService.saveBatch(list);
        return R.success("修改成功");
    }

    /**
     * 拉去菜品列表
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    public R<List<SetmealDto>> list(Setmeal setmeal){
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Setmeal::getCategoryId,setmeal.getCategoryId());
        queryWrapper.eq(Setmeal::getStatus,setmeal.getStatus());

        final List<Setmeal> setmealList = setmealService.list(queryWrapper);
        final List<SetmealDto> setmealDtoList = setmealList.stream().map((item) -> {
            final SetmealDto dto = new SetmealDto();
            BeanUtils.copyProperties(item, dto);
            LambdaQueryWrapper<SetmealDish> dishQueryWrapper = new LambdaQueryWrapper<>();
            dishQueryWrapper.eq(SetmealDish::getSetmealId, item.getId());
            dishQueryWrapper.orderByDesc(SetmealDish::getUpdateTime);
            final List<SetmealDish> dishList = setmealDishService.list(dishQueryWrapper);
            dto.setSetmealDishes(dishList);
            return dto;
        }).collect(Collectors.toList());
        return R.success(setmealDtoList);
    }
}
