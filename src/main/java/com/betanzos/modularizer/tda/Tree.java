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
import java.util.function.BiPredicate;

/**
 * @author Eduardo Betanzos
 * @since 1.0
 */
public class Tree<T> {

    private TreeNode<T> root;

    public Tree(TreeNode<T> root) {
        this.root = root;
    }

    public boolean isEmpty() {
        return root == null;
    }

    /**
     * Elimina el nodo {@code node} del árbol. Si {@code node} tiene hijos, estos
     * pasan a ser hijos de su padre.
     * 
     * @param node - Nodo a eliminar
     * @return Los datos del nodo eliminado
     */
    public T remove(TreeNode<T> node) {
        TreeNode<T> father = getFather(node);
        if (father != null) {
            father.getChildren().addAll(node.getChildren());
            return node.getData();
        }
        return null;
    }

    public TreeNode<T> getRoot() {
        return root;
    }

    public List<TreeNode<T>> getPreOrderNodeList() {
        List<TreeNode<T>> nodeList = new ArrayList<>();
        if (!isEmpty()) {
            nodeList.add(root);
            for (int i = 0; i < nodeList.size(); i++) {
                TreeNode<T> node = nodeList.get(i);
                
                if (node.degree() > 0) {
                    nodeList.addAll(node.getChildren());
                }
            }
        }
        return nodeList;
    }

    public List<TreeNode<T>> getLeaves() {
        List<TreeNode<T>> nodeList = getPreOrderNodeList();
        List<TreeNode<T>> leavesList = new ArrayList<>();
        for (TreeNode<T> node : nodeList) {
            if (node.isLeaf()) {
                leavesList.add(node);
            }
        }
        
        return leavesList;
    }

    public int getTreeDegree() {
        List<TreeNode<T>> nodeList = getPreOrderNodeList();
        int treeDegree = 0;
        for (TreeNode<T> node : nodeList) {
            if (node.degree() > treeDegree) {
                treeDegree = node.degree();
            }
        }
        
        return treeDegree;
    }

    public int getTreeLevel() {
        List<TreeNode<T>> nodeList = getPreOrderNodeList();
        int treeLevel = 0;
        for (TreeNode<T> node : nodeList) {
            int nodeLevel = getNodeLevel(node);
            if (nodeLevel > treeLevel) {
                treeLevel = nodeLevel;
            }
        }
        return treeLevel;
    }

    public TreeNode<T> getFather(TreeNode<T> node) {
        TreeNode<T> father = null;
        if (!node.equals(root)) {
            List<TreeNode<T>> nodeList = getPreOrderNodeList();
            boolean found = false;
            int i = 0;
            while (!found && i < nodeList.size()) {
                TreeNode<T> candidateFather = nodeList.get(i);
                List<TreeNode<T>> children = candidateFather.getChildren();
                
                int j = 0;
                while (!found && j < children.size()) {
                    TreeNode<T> item = children.get(j);
                    if (item.equals(node)) {
                        found = true;
                    }
                    j++;
                }
                
                if (found) {
                    father = candidateFather;
                }
                i++;
            }
        }
        
        return father;
    }

    public int getNodeLevel(TreeNode<T> node) {
        if (node.equals(root)) {
            return 0;
        } else {
            return getNodeLevel(getFather(node)) + 1;
        }
    }

    public List<TreeNode<T>> getNodesAtLevel(int level) {
        List<TreeNode<T>> nodeList = getPreOrderNodeList();
        List<TreeNode<T>> founds = new ArrayList<>();
        for (TreeNode<T> node : nodeList) {
            if (getNodeLevel(node) == level) {
                founds.add(node);
            }
        }
        return founds;
    }

    public TreeNode<T> findNodeByData(T data) {
        List<TreeNode<T>> nodeList = getPreOrderNodeList();
        int i = 0;
        while (i < nodeList.size()) {
            TreeNode<T> node = nodeList.get(i);
            if (node.getData().equals(data)) {
                return node;
            }
            i++;
        }
        return null;
    }

    public TreeNode<T> findNodeByData(T data, BiPredicate<T, T> predicate) {
        List<TreeNode<T>> nodeList = getPreOrderNodeList();

        int i = 0;
        while (i < nodeList.size()) {
            TreeNode<T> node = nodeList.get(i);
            if (predicate.test(node.getData(), data)) {
                return node;
            }
            i++;
        }
        return null;
    }

    public void removeSubtree(TreeNode<T> node) {
        TreeNode<T> father = getFather(node);
        if (father != null) {
            father.getChildren().remove(node);
        }
    }
}
