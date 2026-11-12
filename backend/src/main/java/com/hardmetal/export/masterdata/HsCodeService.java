package com.hardmetal.export.masterdata;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hardmetal.export.shared.web.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

/**
 * HS 编码库。《接口文档》12.2 / 12.3。
 */
@Service
public class HsCodeService {

    private final HsCodeMapper hsCodeMapper;

    public HsCodeService(HsCodeMapper hsCodeMapper) {
        this.hsCodeMapper = hsCodeMapper;
    }

    public PageResult<HsCodeView> page(int page, int size, String q, String chapter) {
        int p = PageResult.normalizePage(page);
        int s = PageResult.normalizeSize(size);
        var wrapper = Wrappers.<HsCode>lambdaQuery();
        if (StringUtils.hasText(chapter)) {
            wrapper.likeRight(HsCode::getCode, chapter.trim());
        }
        if (StringUtils.hasText(q)) {
            String kw = q.trim();
            wrapper.and(w -> w.likeRight(HsCode::getCode, kw)
                    .or().like(HsCode::getNameCn, kw)
                    .or().like(HsCode::getNameEn, kw));
        }
        wrapper.orderByAsc(HsCode::getCode);
        IPage<HsCode> result = hsCodeMapper.selectPage(new Page<>(p, s), wrapper);
        List<HsCodeView> items = result.getRecords().stream().map(HsCodeService::toView).toList();
        return PageResult.of(items, result.getTotal(), p, s);
    }

    /**
     * 按编码 upsert。未传字段保持原值；新建时缺省为零/空。
     */
    @Transactional
    public HsCodeSaved upsert(HsCodeUpsertRequest request) {
        HsCode entity = hsCodeMapper.selectById(request.code());
        boolean isNew = entity == null;
        if (isNew) {
            entity = new HsCode();
            entity.setCode(request.code());
        }
        entity.setNameCn(request.nameCn());
        if (request.exportTariff() != null) {
            entity.setExportDutyRate(scale6(request.exportTariff()));
        }
        if (request.refundRate() != null) {
            entity.setExportRebateRate(scale6(request.refundRate()));
        }
        if (request.regulatoryCondition() != null) {
            entity.setSupervisionCondition(request.regulatoryCondition());
        }
        if (request.declarationElements() != null) {
            entity.setDeclarationElements(request.declarationElements().toArray(String[]::new));
        }
        if (request.classifyBasis() != null) {
            entity.setClassificationBasis(request.classifyBasis());
        }
        if (isNew) {
            entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            hsCodeMapper.insert(entity);
        } else {
            entity.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            hsCodeMapper.updateById(entity);
        }
        return new HsCodeSaved(entity.getCode());
    }

    private static BigDecimal scale6(BigDecimal value) {
        return value.setScale(6, RoundingMode.HALF_UP);
    }

    private static HsCodeView toView(HsCode e) {
        String unit1 = StringUtils.hasText(e.getLegalUnit1()) ? e.getLegalUnit1() : e.getUnit();
        String[] elements = e.getDeclarationElements();
        return new HsCodeView(
                e.getCode(),
                e.getNameCn(),
                e.getNameEn(),
                unit1,
                e.getExportDutyRate(),
                e.getExportRebateRate(),
                e.getSupervisionCondition(),
                e.getInspectionCategory(),
                elements == null ? null : Arrays.asList(elements),
                e.getClassificationBasis());
    }
}