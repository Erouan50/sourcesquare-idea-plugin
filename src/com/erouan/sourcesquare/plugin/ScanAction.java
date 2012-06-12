package com.erouan.sourcesquare.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.Library;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Antoine ROUAZE <antoine.rouaze AT zenika.com>
 */
public class ScanAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        Set<LibraryOrderEntry> libraries = new HashSet<LibraryOrderEntry>();
        Module[] modules = ModuleManager.getInstance(e.getProject()).getModules();
        for (Module module : modules) {
            OrderEntry[] orderEntries = ModuleRootManager.getInstance(module).getOrderEntries();
            for (OrderEntry orderEntry : orderEntries) {
                if (orderEntry instanceof LibraryOrderEntry) {
                    LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) orderEntry;
                    libraries.add(libraryOrderEntry);
                }
            }
        }
        System.out.println(libraries.size());
    }

    private void startScan(Set<LibraryOrderEntry> libraries) {

    }
}
