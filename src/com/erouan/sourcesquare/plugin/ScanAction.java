package com.erouan.sourcesquare.plugin;

import com.antelink.sourcesquare.client.scan.ScanStatus;
import com.antelink.sourcesquare.client.scan.ScanStatusManager;
import com.antelink.sourcesquare.client.scan.SourceSquareEngine;
import com.antelink.sourcesquare.client.scan.SourceSquareFSWalker;
import com.antelink.sourcesquare.event.base.ClientEventHandler;
import com.antelink.sourcesquare.event.base.ClientEventType;
import com.antelink.sourcesquare.event.base.EventBus;
import com.antelink.sourcesquare.event.events.FilesScannedEvent;
import com.antelink.sourcesquare.event.events.OSFilesFoundEvent;
import com.antelink.sourcesquare.event.events.ScanCompleteEvent;
import com.antelink.sourcesquare.event.events.StartScanEvent;
import com.antelink.sourcesquare.event.handlers.FilesScannedEventHandler;
import com.antelink.sourcesquare.event.handlers.OSFilesFoundEventHandler;
import com.antelink.sourcesquare.event.handlers.ScanCompleteEventHandler;
import com.antelink.sourcesquare.query.AntepediaQuery;
import com.antelink.sourcesquare.results.ResultBuilder;
import com.antelink.sourcesquare.results.TreeMapBuilder;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Antoine ROUAZE <antoine.rouaze AT zenika.com>
 */
public class ScanAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        String projectPath = project.getBasePath();
        startScan(projectPath);
    }

    private void startScan(String projectPath) {
        final EventBus eventBus = new EventBus();
        AntepediaQuery query = new AntepediaQuery();
        SourceSquareEngine engine = new SourceSquareEngine(eventBus, query);
        ScanStatusManager manager = new ScanStatusManager(eventBus);
        manager.bind();
        TreeMapBuilder treemap = new TreeMapBuilder(eventBus);
        treemap.bind();
        ResultBuilder builder = new ResultBuilder(eventBus, treemap);
        builder.bind();
        final SourceSquareFSWalker walker = new SourceSquareFSWalker(engine, eventBus, treemap);
        walker.bind();
        File file = new File(projectPath);
        eventBus.fireEvent(new StartScanEvent(file));
        eventBus.addHandler(ScanCompleteEvent.TYPE, new ScanCompleteEventHandler() {
            @Override
            public void handle(ArrayList<Integer> levels) {
                System.out.println("Files scanned at the end: " + ScanStatus.INSTANCE.getNbFilesToScan());
            }

            @Override
            public String getId() {
                return getClass().getCanonicalName() + ": " + ScanCompleteEventHandler.class.getName();
            }
        });
        eventBus.addHandler(FilesScannedEvent.TYPE, new FilesScannedEventHandler() {
            @Override
            public void handle(int count) {
                System.out.println("Files scanned: " + ScanStatus.INSTANCE.getNbFilesScanned());
            }

            @Override
            public String getId() {
                return getClass().getCanonicalName() + ": " + FilesScannedEventHandler.class.getName();
            }
        });
    }
}
