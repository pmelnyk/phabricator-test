package com.andrasta.dashi.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class CyclicBufferTest {
    @Test
    public void testCyclicBuffer() throws Exception {
        CyclicBuffer<String> buffer = new CyclicBuffer<>(3);
        List<String> content = buffer.asList();
        Assert.assertTrue(content.size() == 0);

        buffer.add("a");
        content = buffer.asList();
        Assert.assertTrue(content.size() == 1);
        Assert.assertTrue(content.get(0).equals("a"));

        buffer.add("b");
        content = buffer.asList();
        Assert.assertTrue(content.size() == 2);
        Assert.assertTrue(content.get(1).equals("a"));
        Assert.assertTrue(content.get(0).equals("b"));

        buffer.add("c");
        content = buffer.asList();
        Assert.assertTrue(content.size() == 3);
        Assert.assertTrue(content.get(2).equals("a"));
        Assert.assertTrue(content.get(1).equals("b"));
        Assert.assertTrue(content.get(0).equals("c"));

        buffer.add("d");
        content = buffer.asList();
        Assert.assertTrue(content.size() == 3);
        Assert.assertTrue(content.get(2).equals("b"));
        Assert.assertTrue(content.get(1).equals("c"));
        Assert.assertTrue(content.get(0).equals("d"));

        buffer.add("e");
        content = buffer.asList();
        Assert.assertTrue(content.size() == 3);
        Assert.assertTrue(content.get(2).equals("c"));
        Assert.assertTrue(content.get(1).equals("d"));
        Assert.assertTrue(content.get(0).equals("e"));

        buffer.reset();
        content = buffer.asList();
        Assert.assertTrue(content.size() == 0);

        buffer.add("a");
        content = buffer.asList();
        Assert.assertTrue(content.size() == 1);
        Assert.assertTrue(content.get(0).equals("a"));

        buffer.add("b");
        content = buffer.asList();
        Assert.assertTrue(content.size() == 2);
        Assert.assertTrue(content.get(1).equals("a"));
        Assert.assertTrue(content.get(0).equals("b"));
    }
}