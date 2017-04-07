////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2017 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.checks.coding;

import static com.puppycrawl.tools.checkstyle.checks.coding.AbstractSuperCheck.MSG_KEY;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.puppycrawl.tools.checkstyle.BaseCheckTestSupport;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;

public class SuperCloneCheckTest
    extends BaseCheckTestSupport {
    @Override
    protected String getPath(String filename) throws IOException {
        return super.getPath("checks" + File.separator
                + "coding" + File.separator + filename);
    }

    @Override
    protected String getNonCompilablePath(String filename) throws IOException {
        return super.getNonCompilablePath("checks" + File.separator
                + "coding" + File.separator + filename);
    }

    @Test
    public void testIt() throws Exception {
        final DefaultConfiguration checkConfig =
            createCheckConfig(SuperCloneCheck.class);
        final String[] expected = {
            "27:19: " + getCheckMessage(MSG_KEY, "clone", "super.clone"),
            "35:19: " + getCheckMessage(MSG_KEY, "clone", "super.clone"),
            "60:48: " + getCheckMessage(MSG_KEY, "clone", "super.clone"),
        };
        verify(checkConfig, getPath("InputClone.java"), expected);
    }

    @Test
    public void testAnotherInputFile() throws Exception {
        final DefaultConfiguration checkConfig =
            createCheckConfig(SuperCloneCheck.class);
        final String[] expected = {
            "9:17: " + getCheckMessage(MSG_KEY, "clone", "super.clone"),
        };
        verify(checkConfig, getPath("InputSuperClone.java"), expected);
    }

    @Test
    public void testTokensNotNull() {
        final SuperCloneCheck check = new SuperCloneCheck();
        Assert.assertNotNull(check.getAcceptableTokens());
        Assert.assertNotNull(check.getDefaultTokens());
        Assert.assertNotNull(check.getRequiredTokens());
    }
}
