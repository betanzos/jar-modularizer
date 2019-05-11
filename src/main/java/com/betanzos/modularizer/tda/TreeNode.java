/**
 * MIT License
 *
 * Copyright (c) 2019 Eduardo E. Betanzos Morales
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.betanzos.modularizer.tda;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eduardo Betanzos
 * @since 1.0
 */
public class TreeNode<T> {

    private T data;
    private List<TreeNode<T>> children;

    public TreeNode(T data) {
        if (data == null) {
            throw new IllegalArgumentException("'data' can´t be null");
        }
        
        this.data = data;
        children = new ArrayList<>();
    }
    
    public T getData() {
        return data;
    }

    public int degree() {
        return children.size();
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public List<TreeNode<T>> getChildren() {
        return children;
    }

    public int getChildIndex(TreeNode<T> node) {
        boolean found = false;
        int i = 0;
        while (!found && i <  children.size()) {
            if (children.get(i) == node) {
                found = true;
            } else {
                i++;
            }
        }
        
        return found ? i : -1;
    }

    public TreeNode<T> getChildAt(int index) {
        return children.get(index);
    }
}
