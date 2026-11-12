package com.hardmetal.export.masterdata;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hardmetal.export.shared.exception.BizException;
import com.hardmetal.export.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * 港口/机场/陆路口岸基础数据。《接口文档》12.4。
 * <p>类型取值统一为 {@code seaport / airport / landport}（库与 DDL CHECK 一致）。
 */
@Service
public class PortService {

    private static final Set<String> TYPES = Set.of("seaport", "airport", "landport");

    private final PortMapper portMapper;

    public PortService(PortMapper portMapper) {
        this.portMapper = portMapper;
    }

    public List<PortView> list(String q, String type, String country) {
        String t = StringUtils.hasText(type) ? type.trim().toLowerCase() : null;
        if (t != null && !TYPES.contains(t)) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "港口类型只允许 seaport / airport / landport，当前：" + type);
        }
        var wrapper = Wrappers.<Port>lambdaQuery()
                .eq(t != null, Port::getPortType, t)
                .eq(StringUtils.hasText(country), Port::getCountryCode,
                        country == null ? null : country.trim().toUpperCase());
        if (StringUtils.hasText(q)) {
            String kw = q.trim();
            wrapper.and(w -> w.likeRight(Port::getCode, kw)
                    .or().like(Port::getNameCn, kw)
                    .or().like(Port::getNameEn, kw));
        }
        wrapper.orderByAsc(Port::getCode);
        return portMapper.selectList(wrapper).stream()
                .map(p -> new PortView(p.getCode(), p.getNameEn(), p.getNameCn(), p.getCountryCode(),
                        p.getPortType()))
                .toList();
    }
}