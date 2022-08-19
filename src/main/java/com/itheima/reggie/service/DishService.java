package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;

public interface DishService extends IService<Dish> {

    //新增菜品，同时插入菜品对应的口味数据，需要操作两张表：dish，dish_flavor
    public void saveWithFlavor(DishDto dishDto);

    //根据菜品id查询菜品信息和对应的口味信息
    public DishDto getByIdWithFlavor(Long id);
    //更新菜品信息
    public void updateWithFlavor(DishDto dishDto);
    //设置菜品状态
    public void status(int status,Long id);
    //删除菜品
    public void delete(Long id);
}
