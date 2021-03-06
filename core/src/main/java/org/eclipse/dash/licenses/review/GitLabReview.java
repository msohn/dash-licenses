/*************************************************************************
 * Copyright (c) 2020,2021 The Eclipse Foundation and others.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution, and is available at https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *************************************************************************/
package org.eclipse.dash.licenses.review;

import org.eclipse.dash.licenses.IContentData;
import org.eclipse.dash.licenses.IContentId;
import org.eclipse.dash.licenses.LicenseData;
import org.eclipse.dash.licenses.clearlydefined.ClearlyDefinedContentData;
import org.eclipse.dash.licenses.context.IContext;

public class GitLabReview {
	private LicenseData licenseData;
	private IContext context;

	public GitLabReview(IContext context, LicenseData licenseData) {
		this.context = context;
		this.licenseData = licenseData;
	}

	public String getTitle() {
		return getContentId().toString();
	}

	public String getLabels() {
		return "Review Needed";
	}

	public String getDescription() {
		StringBuilder builder = new StringBuilder();
		builder.append(String.format("%s\n\n", licenseData.getId()));

		builder.append(String.format("Project: [%s](https://projects.eclipse.org/projects/%s)\n",
				context.getSettings().getProjectId(), context.getSettings().getProjectId()));

		licenseData.contentData().forEach(data -> describeItem(data, builder));

		String searchUrl = IPZillaSearchBuilder.build(licenseData.getId());
		if (searchUrl != null) {
			builder.append("\n");
			builder.append(String.format("[Search IPZilla](%s)\n", searchUrl));
		}

		// FIXME This is clunky
		var mavenCentralUrl = getMavenCentralUrl();
		if (mavenCentralUrl != null) {
			builder.append("\n");
			builder.append(String.format("[Maven Central](%s)\n", mavenCentralUrl));

			var source = getVerifiedMavenCentralSourceUrl();
			if (source != null) {
				builder.append(String.format("  - [Source](%s) from Maven Central\n", source));
			}
		}

		// If the id is recognised as a package in the npmjs.com repository, capture
		// as much helpful information as we can.
		var npmjsPackage = context.getNpmjsService().getPackage(licenseData.getId());
		if (npmjsPackage != null) {
			builder.append("\n");
			builder.append(String.format("[npmjs.com](%s)\n", npmjsPackage.getUrl()));
			if (npmjsPackage.getLicense() != null)
				builder.append(String.format("  - License: %s\n", npmjsPackage.getLicense()));
			if (npmjsPackage.getDistributionUrl() != null)
				builder.append(String.format("  - [Distribution](%s)\n", npmjsPackage.getDistributionUrl()));
			if (npmjsPackage.getRepositoryUrl() != null)
				builder.append(String.format("  - [Repository](%s)\n", npmjsPackage.getRepositoryUrl()));
			if (npmjsPackage.getSourceUrl() != null)
				builder.append(String.format("  - [Source](%s)\n", npmjsPackage.getSourceUrl()));
		}

		return builder.toString();
	}

	/**
	 * THis method writes potentially helpful information to make the intellectual
	 * review process as easy as possible to the output writer.
	 * 
	 * @param data
	 */
	private void describeItem(IContentData data, StringBuilder output) {
		// FIXME This is clunky

		output.append("\n");
		String authority = data.getAuthority();
		if (data.getUrl() != null)
			authority = String.format("[%s](%s)", authority, data.getUrl());
		output.append(String.format("%s\n", authority));
		output.append(String.format("  - Declared: %s (%d)\n", data.getLicense(), data.getScore()));
		switch (data.getAuthority()) {
		case ClearlyDefinedContentData.CLEARLYDEFINED:
			((ClearlyDefinedContentData) data).discoveredLicenses()
					.forEach(license -> output.append("  - Discovered: " + license).append('\n'));
		};
	}

	public boolean isFromMavenCentral() {
		return "mavencentral".equals(getContentId().getSource());
	}

	public boolean isFromNpmjs() {
		return "npmjs".equals(getContentId().getSource());
	}

	public String getMavenCentralUrl() {
		if (!isFromMavenCentral()) {
			return null;
		}

		var id = getContentId();

		return String.format("https://search.maven.org/artifact/%s/%s/%s/jar", id.getNamespace(), id.getName(),
				id.getVersion());
	}

	public String getNpmjsUrl() {
		var id = getContentId();
		if (!isFromNpmjs()) {
			return null;
		}

		var npmId = new StringBuilder();
		if (!"-".equals(id.getNamespace())) {
			npmId.append(id.getNamespace());
			npmId.append('/');
		}
		npmId.append(id.getName());

		return String.format("https://www.npmjs.com/package/%s/v/%s", npmId.toString(), id.getVersion());
	}

	public String getVerifiedMavenCentralSourceUrl() {
		var url = getMavenCentralSourceUrl();
		if (url == null) {
			return null;
		}

		if (context.getHttpClientService().remoteFileExists(url)) {
			return url;
		}

		return null;
	}

	public String getMavenCentralSourceUrl() {
		if (!isFromMavenCentral()) {
			return null;
		}

		var id = getContentId();

		// FIXME Validate that this file pattern is correct.
		// This pattern was observed and appears to be accurate.
		var url = "https://search.maven.org/remotecontent?filepath={groupPath}/{artifactid}/{version}/{artifactid}-{version}-sources.jar";
		url = url.replace("{groupPath}", id.getNamespace().replace('.', '/'));
		url = url.replace("{artifactid}", id.getName());
		url = url.replace("{version}", id.getVersion());

		return url;
	}

	private IContentId getContentId() {
		return licenseData.getId();
	}
}
