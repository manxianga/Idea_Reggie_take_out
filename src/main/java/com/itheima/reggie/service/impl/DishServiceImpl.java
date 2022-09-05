package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private CategoryService categoryService;
    /**
     * 新增菜品，同时保存对应的口味数据
     * @param dishDto
     */
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {
        //保存菜品的基本信息到菜品表dish
        this.save(dishDto);

        //获取dishId,并给dishDto重新赋值
        Long dishId = dishDto.getId();
        List<DishFlavor> flavors = dishDto.getFlavors().stream().map((item)->{
           item.setDishId(dishId);
           return item;
        }).collect(Collectors.toList());

        //保存菜品口味数据到菜品口味表dish_flavor
        dishFlavorService.saveBatch(flavors);//批量保存
    }

    /**
     * 根据菜品id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    public DishDto getByIdWithFlavor(Long id) {
        //查询菜品基本信息，从dish表中查询
        final Dish dish = this.getById(id);
        final DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish,dishDto);//数据copy

        //查询当前菜品对应的口味信息，从dish_flavor表中查询
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dish.getId());
        final List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
        dishDto.setFlavors(flavors);
        //查询菜品分类的名称
        final Category category = categoryService.getById(dish.getCategoryId());
        if(category != null){
            dishDto.setCategoryName(category.getName());//给名称赋值
        }
        return dishDto;
    }

    /**
     * 刷新菜品及其口味
     * @param dishDto
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
        //更新dish表基本信息
        this.updateById(dishDto);
        //清理当前菜品对应口味数据-dish_flavor表的deltet操作
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(true,DishFlavor::getDishId,dishDto.getId());
        dishFlavorService.remove(queryWrapper);

        //获取dishId,并给dishDto重新赋值
        Long dishId = dishDto.getId();
        List<DishFlavor> flavors = dishDto.getFlavors().stream().map((item)->{
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());

        //保存菜品口味数据到菜品口味表dish_flavor
        dishFlavorService.saveBatch(flavors);//批量保存
    }

    /**
     * 删除菜品
     * @param id
     */
    @Override
    public void delete(Long id) {
        this.removeById(id);
    }

    /**
     * 设置菜品状态
     * @param status
     * @param id
     */
    @Override
    public void status(int status, Long id) {
        final Dish dish = this.getById(id);
        dish.setStatus(status);
        this.updateById(dish);
    }
}
