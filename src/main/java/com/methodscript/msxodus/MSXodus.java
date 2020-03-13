package com.methodscript.msxodus;

import com.laytonsmith.PureUtilities.SimpleVersion;
import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.core.extensions.AbstractExtension;
import com.laytonsmith.core.extensions.MSExtension;

/**
 *
 */
@MSExtension("MSXodus")
public class MSXodus extends AbstractExtension {
	public static final Version v1_0_0 = new SimpleVersion(1, 0, 0, "SNAPSHOT");

	@Override
	public Version getVersion() {
		return v1_0_0;
	}

}
