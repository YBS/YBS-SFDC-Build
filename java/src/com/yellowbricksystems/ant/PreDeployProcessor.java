/*
Copyright (c) 2015 Yellow Brick Systems LLC

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

	PreDeployProcessor
	
	This class defines a custom Ant Task which is used to update the Metadata
	files prior to a deployment. There are currently no updates that need to be
	made prior to deployment, so this task is just a stub.  At one point it was
	necessary to update the usernames when deploying to a Sandbox but that logic
	has been removed.
	
	Author:  Jeff Bohanek (jeff@yellowbricksystems.com)
 */
package com.yellowbricksystems.ant;

import org.apache.tools.ant.BuildException;

public class PreDeployProcessor extends SalesforceTask {

	protected String deployTarget;

	public String getDeployTarget() {
		return deployTarget;
	}

	public void setDeployTarget(String deployTarget) {
		this.deployTarget = deployTarget;
	}

	@Override
	public void init() throws BuildException {

		super.init();
	}

	@Override
	public void execute() throws BuildException {
		if ((deployTarget == null) || (deployTarget.length() < 1)) {
			throw new BuildException("Please specify a deployTarget directory to process.");
		}

		processPreDeploy();
	}

	/**
	 * Perform the Post-Retrieve processing on the retrieveTarget directory.
	 */
	protected void processPreDeploy() {
		try {
			// We no longer need to fix the usernames, so now this is just a stub
			
		} catch (Exception ex) {
			throw new BuildException("Error trying to perform pre-deploy processing", ex);
		}
	}

}
