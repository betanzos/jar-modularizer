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
package com.betanzos.modularizer.pojo;

import java.util.Objects;

/**
 * @author Eduardo Betanzos
 * @since 1.0
 */
public class Artifact {
    private String name;
    private Module module;

    public Artifact() {
    }

    public Artifact(String name, Module module) {
        this.name = name;
        this.module = module;
    }

    public String getName() {
        return name;
    }

    public Artifact setName(String name) {
        this.name = name;
        return this;
    }

    public Module getModule() {
        return module;
    }

    public Artifact setModule(Module module) {
        this.module = module;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Artifact)) {
            return false;
        }

        Artifact objArtifact = (Artifact) obj;

        return this.name.equals(objArtifact.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
