package com.docmind.backend.service;

import com.docmind.backend.dto.SourceInfo.PageRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageMapTest {

    // Page 1 is "aaa", page 2 is "bbb", page 3 is empty, page 4 is "ddd". A page break ends every page.
    private static final String TEXT = "aaa\fbbb\f\fddd\f";

    @Test
    void aPageBreakBelongsToThePageItEnds() {
        PageMap pages = new PageMap(TEXT);

        assertThat(pages.pageOf(0)).isEqualTo(1);
        assertThat(pages.pageOf(3)).isEqualTo(1); // the break itself
    }

    @Test
    void findsThePageOfAPosition() {
        PageMap pages = new PageMap(TEXT);

        assertThat(pages.pageOf(4)).isEqualTo(2);
        assertThat(pages.pageOf(6)).isEqualTo(2);
        assertThat(pages.pageOf(9)).isEqualTo(4);
    }

    @Test
    void anEmptyPageKeepsItsPlaceSoLaterPageNumbersStayRight() {
        PageMap pages = new PageMap(TEXT);

        assertThat(pages.pageOf(8)).isEqualTo(3); // the break of the empty page
        assertThat(pages.pageOf(9)).isEqualTo(4);
    }

    @Test
    void aTextWithoutPageBreaksHasNoPageInformation() {
        assertThat(new PageMap("an older document").hasPages()).isFalse();
        assertThat(new PageMap(TEXT).hasPages()).isTrue();
    }

    @Test
    void neighbouringPagesAreJoinedIntoOneRange() {
        List<DocumentRetriever.Run> runs = List.of(
                new DocumentRetriever.Run(0, 3),   // page 1
                new DocumentRetriever.Run(4, 6),   // page 2
                new DocumentRetriever.Run(9, 12)); // page 4

        List<PageRange> pages = new PageMap(TEXT).pagesOf(runs);

        assertThat(pages).containsExactly(new PageRange(1, 2), new PageRange(4, 4));
    }

    @Test
    void aPieceThatCrossesAPageBreakCoversBothPages() {
        List<PageRange> pages = new PageMap(TEXT).pagesOf(List.of(new DocumentRetriever.Run(2, 6)));

        assertThat(pages).containsExactly(new PageRange(1, 2));
    }
}