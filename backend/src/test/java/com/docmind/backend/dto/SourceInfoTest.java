package com.docmind.backend.dto;

import com.docmind.backend.dto.SourceInfo.PageRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SourceInfoTest {

    @Test
    void wholeAndExcerptsAreSavedUnderTheirName() {
        assertThat(SourceInfo.whole().toStorage()).isEqualTo("WHOLE");
        assertThat(SourceInfo.excerpts().toStorage()).isEqualTo("EXCERPTS");
    }

    @Test
    void pagesAreSavedAsRanges() {
        SourceInfo sources = SourceInfo.pages(List.of(new PageRange(1, 1), new PageRange(13, 14)));

        assertThat(sources.toStorage()).isEqualTo("PAGES 1-1,13-14");
    }

    @Test
    void whatWasSavedCanBeReadBack() {
        SourceInfo pages = SourceInfo.pages(List.of(new PageRange(2, 3), new PageRange(9, 9)));

        assertThat(SourceInfo.fromStorage(SourceInfo.whole().toStorage())).isEqualTo(SourceInfo.whole());
        assertThat(SourceInfo.fromStorage(SourceInfo.excerpts().toStorage())).isEqualTo(SourceInfo.excerpts());
        assertThat(SourceInfo.fromStorage(pages.toStorage())).isEqualTo(pages);
    }

    @Test
    void emptyOrBrokenTextGivesNoSources() {
        assertThat(SourceInfo.fromStorage(null)).isNull();
        assertThat(SourceInfo.fromStorage("   ")).isNull();
        assertThat(SourceInfo.fromStorage("something else")).isNull();
        assertThat(SourceInfo.fromStorage("PAGES x-y")).isNull();
        assertThat(SourceInfo.fromStorage("PAGES 4")).isNull();
    }
}