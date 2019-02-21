package com.test.ginopaydemo;

import java.util.List;

public class TLV implements Comparable<TLV> {

    public String tag;
    public String length;
    public String value;

    public boolean isNested;
    public List<TLV> tlvList;

    @Override
    public int compareTo(TLV tlv) {
        return tag.compareToIgnoreCase(tlv.tag);
    }

}