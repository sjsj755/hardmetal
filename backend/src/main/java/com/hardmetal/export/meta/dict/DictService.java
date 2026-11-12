package com.hardmetal.export.meta.dict;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hardmetal.export.shared.exception.BizException;
import com.hardmetal.export.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典查询。sys_dict 是枚举取值的唯一运行期来源：
 * 前端下拉、联调页、冒烟测试都走这里，避免各处硬编码漂移。
 */
@Service
public class DictService {

    private final SysDictMapper mapper;

    public DictService(SysDictMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按类型返回字典项，按 sort_no、id 升序。
     *
     * @throws BizException 类型不存在或全部已删除时抛 40401
     */
    public List<DictItem> list(String type) {
        List<SysDict> rows = mapper.selectList(Wrappers.<SysDict>lambdaQuery()
                .eq(SysDict::getDictType, type)
                .orderByAsc(SysDict::getSortNo)
                .orderByAsc(SysDict::getId));
        if (rows.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "字典类型不存在：" + type);
        }
        return rows.stream()
                .map(r -> new DictItem(r.getDictKey(), r.getDictLabel(), null, r.getSortNo()))
                .toList();
    }

    /**
     * 该字典类型的全部可选 value，按 sort 升序。服务端校验直接用这份数据，
     * 字典改了，校验与提示文案随之改变，不需要再改 Java 常量。
     */
    public List<String> values(String type) {
        return list(type).stream().map(DictItem::value).toList();
    }

    /**
     * 校验枚举值属于该字典。
     *
     * @throws BizException 40001，提示里带上字典当前允许的全部取值
     */
    public void assertAllowed(String type, String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, field + " 不能为空");
        }
        List<String> allowed = values(type);
        if (!allowed.contains(value)) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    field + " 只允许 " + String.join("/", allowed) + "，当前：" + value);
        }
    }
}