package org.ecorefs.ipfs;

import io.ipfs.api.IPFS;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceFactoryImpl;

public class IPFSResourceFactoryImpl extends ResourceFactoryImpl {
    private final IPFS ipfs;

    public IPFSResourceFactoryImpl(IPFS ipfs) {
        this.ipfs = ipfs;
    }

    @Override
    public Resource createResource(URI uri) {
        return new IPFSResourceImpl(uri, ipfs);
    }
}
