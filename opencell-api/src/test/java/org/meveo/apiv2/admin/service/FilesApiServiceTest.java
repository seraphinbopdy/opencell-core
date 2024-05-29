package org.meveo.apiv2.admin.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.meveo.apiv2.admin.ImmutableFilesPagingAndFiltering;
import org.meveo.commons.utils.ParamBeanFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FilesApiServiceTest {

    @InjectMocks
    private FilesApiService filesApiService;
    
    @Mock
    private ParamBeanFactory paramBeanFactory;
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    @Before
    public void setup() {
    }


    @Test
    public void searchFilesInValidDirectory() throws IOException {
        File validDirectory = temporaryFolder.newFolder("validDirectory");
        new File(validDirectory, "file.criteria1.txt").createNewFile();
        new File(validDirectory, "file.criteria2.txt").createNewFile();
        
        when(paramBeanFactory.getDefaultChrootDir()).thenReturn(validDirectory.getPath());

        ImmutableFilesPagingAndFiltering searchConfig = ImmutableFilesPagingAndFiltering.builder()
                                                                                        .limit(10L)
                                                                                        .offset(0L)
                                                                                        .build();

        Map<String, Object> result = filesApiService.searchFiles(searchConfig);

        assertNotNull(result);
        assertTrue(result.containsKey("total"));
        assertEquals(2L, result.get("total"));
        assertTrue(result.containsKey("limit"));
        assertTrue(result.containsKey("offset"));
        assertTrue(result.containsKey("size"));
        assertTrue(result.containsKey("data"));
        assertNotNull(result.get("data"));
        assertEquals(2, ((List<?>) result.get("data")).size());
    }


    @Test
    public void searchFilesUsingLikeCriteria() throws IOException {
        File validDirectory = temporaryFolder.newFolder("validDirectory");
        new File(validDirectory, "file.criTeria1.txt").createNewFile();
        new File(validDirectory, "file.criTeria2.txt").createNewFile();

        when(paramBeanFactory.getDefaultChrootDir()).thenReturn(validDirectory.getPath());

        ImmutableFilesPagingAndFiltering searchConfig = ImmutableFilesPagingAndFiltering.builder()
                .filters(Map.of("likeCriteria name", "*criTeria1*"))
                                                                                        .limit(10L)
                                                                                        .offset(0L)
                                                                                        .build();

        Map<String, Object> result = filesApiService.searchFiles(searchConfig);

        assertNotNull(result);
        assertTrue(result.containsKey("total"));
        assertEquals(1L, result.get("total"));
        assertTrue(result.containsKey("limit"));
        assertTrue(result.containsKey("offset"));
        assertTrue(result.containsKey("size"));
        assertTrue(result.containsKey("data"));
        assertNotNull(result.get("data"));
        List<org.meveo.apiv2.admin.File> data = (List<org.meveo.apiv2.admin.File>) result.get("data");
        assertEquals(1, data.size());
        assertEquals("file.criTeria1.txt", data.get(0).getName());
    }

    @Test
    public void shouleReturnEmptySearchFilesUsingLikeCriteria() throws IOException {
        File validDirectory = temporaryFolder.newFolder("validDirectory");
        new File(validDirectory, "file.criTeria1.txt").createNewFile();
        new File(validDirectory, "file.criTeria2.txt").createNewFile();

        when(paramBeanFactory.getDefaultChrootDir()).thenReturn(validDirectory.getPath());

        ImmutableFilesPagingAndFiltering searchConfig = ImmutableFilesPagingAndFiltering.builder()
                                                                                        .filters(Map.of("likeCriteria name", "*criteria1*"))
                                                                                        .limit(10L)
                                                                                        .offset(0L)
                                                                                        .build();

        Map<String, Object> result = filesApiService.searchFiles(searchConfig);

        assertNotNull(result);
        assertTrue(result.containsKey("total"));
        assertEquals(0L, result.get("total"));
        assertTrue(result.containsKey("limit"));
        assertTrue(result.containsKey("offset"));
        assertTrue(result.containsKey("size"));
        assertTrue(result.containsKey("data"));
        assertNotNull(result.get("data"));
        List<org.meveo.apiv2.admin.File> data = (List<org.meveo.apiv2.admin.File>) result.get("data");
        assertEquals(0, data.size());
    }



    @Test
    public void searchFilesUsingLikeCriteriaOrIgnoreCase() throws IOException {
        File validDirectory = temporaryFolder.newFolder("validDirectory");
        new File(validDirectory, "file.criTeria1.txt").createNewFile();
        new File(validDirectory, "file.criTeria2.txt").createNewFile();

        when(paramBeanFactory.getDefaultChrootDir()).thenReturn(validDirectory.getPath());

        ImmutableFilesPagingAndFiltering searchConfig = ImmutableFilesPagingAndFiltering.builder()
                                                                                        .filters(Map.of("likeCriteriaOrIgnoreCase name", "*criteria1*"))
                                                                                        .limit(10L)
                                                                                        .offset(0L)
                                                                                        .build();

        Map<String, Object> result = filesApiService.searchFiles(searchConfig);

        assertNotNull(result);
        assertTrue(result.containsKey("total"));
        assertEquals(1L, result.get("total"));
        assertTrue(result.containsKey("limit"));
        assertTrue(result.containsKey("offset"));
        assertTrue(result.containsKey("size"));
        assertTrue(result.containsKey("data"));
        assertNotNull(result.get("data"));
        List<org.meveo.apiv2.admin.File> data = (List<org.meveo.apiv2.admin.File>) result.get("data");
        assertEquals(1, data.size());
        assertEquals("file.criTeria1.txt", data.get(0).getName());
    }


    @Test
    public void searchFilesUsingToRangeOnSize() throws IOException {
        File validDirectory = temporaryFolder.newFolder("validDirectory");
        File file1 = new File(validDirectory, "file.criTeria1.txt"); // write 128KB file
        file1.createNewFile();
        
        Random random = new Random();
        int fileSize = 128; // 128B
        char[] chars = new char[fileSize];

        for (int i = 0; i < fileSize; i++) {
            // Generate a random character between 'a' and 'z'
            chars[i] = (char) ('a' + random.nextInt(26));
        }

        FileWriter writer = new FileWriter(file1);
        writer.write(chars);
        writer.close();


        File file2 = new File(validDirectory, "file.criTeria2.txt"); // write 64KB file
        file2.createNewFile();

        fileSize = 64; // 64B
        chars = new char[fileSize];

        for (int i = 0; i < fileSize; i++) {
            // Generate a random character between 'a' and 'z'
            chars[i] = (char) ('a' + random.nextInt(26));
        }
        
        writer = new FileWriter(file2);
        writer.write(chars);
        writer.close();

        when(paramBeanFactory.getDefaultChrootDir()).thenReturn(validDirectory.getPath());

        ImmutableFilesPagingAndFiltering searchConfig = ImmutableFilesPagingAndFiltering.builder()
                                                                                        .filters(Map.of("toRange size", "100"))
                                                                                        .limit(10L)
                                                                                        .offset(0L)
                                                                                        .build();

        Map<String, Object> result = filesApiService.searchFiles(searchConfig);

        assertNotNull(result);
        assertTrue(result.containsKey("total"));
        assertEquals(1L, result.get("total"));
        assertTrue(result.containsKey("limit"));
        assertTrue(result.containsKey("offset"));
        assertTrue(result.containsKey("size"));
        assertTrue(result.containsKey("data"));
        assertNotNull(result.get("data"));
        List<org.meveo.apiv2.admin.File> data = (List<org.meveo.apiv2.admin.File>) result.get("data");
        assertEquals(1, data.size());
        assertEquals("file.criTeria2.txt", data.get(0).getName());
    }

    @Test
    public void shouldNotReturnExclusiveSearchFilesUsingToRangeOnSize() throws IOException {
        File validDirectory = temporaryFolder.newFolder("validDirectory");
        File file1 = new File(validDirectory, "file.criTeria1.txt"); // write 128KB file
        file1.createNewFile();

        Random random = new Random();
        int fileSize = 128; // 128B
        char[] chars = new char[fileSize];

        for (int i = 0; i < fileSize; i++) {
            // Generate a random character between 'a' and 'z'
            chars[i] = (char) ('a' + random.nextInt(26));
        }

        FileWriter writer = new FileWriter(file1);
        writer.write(chars);
        writer.close();


        File file2 = new File(validDirectory, "file.criTeria2.txt"); // write 64KB file
        file2.createNewFile();

        fileSize = 64; // 64B
        chars = new char[fileSize];

        for (int i = 0; i < fileSize; i++) {
            // Generate a random character between 'a' and 'z'
            chars[i] = (char) ('a' + random.nextInt(26));
        }

        writer = new FileWriter(file2);
        writer.write(chars);
        writer.close();

        when(paramBeanFactory.getDefaultChrootDir()).thenReturn(validDirectory.getPath());

        ImmutableFilesPagingAndFiltering searchConfig = ImmutableFilesPagingAndFiltering.builder()
                                                                                        .filters(Map.of("toRange size", "64"))
                                                                                        .limit(10L)
                                                                                        .offset(0L)
                                                                                        .build();

        Map<String, Object> result = filesApiService.searchFiles(searchConfig);

        assertNotNull(result);
        assertTrue(result.containsKey("total"));
        assertEquals(0L, result.get("total"));
        assertTrue(result.containsKey("limit"));
        assertTrue(result.containsKey("offset"));
        assertTrue(result.containsKey("size"));
        assertTrue(result.containsKey("data"));
        assertNotNull(result.get("data"));
        List<org.meveo.apiv2.admin.File> data = (List<org.meveo.apiv2.admin.File>) result.get("data");
        assertEquals(0, data.size());
    }

    @Test
    public void searchFilesUsingToRangeOnDate() throws IOException {
        File validDirectory = temporaryFolder.newFolder("validDirectory");
        File file1 = new File(validDirectory, "file.criTeria1.txt"); // write 128KB file
        file1.createNewFile();

        long time = System.currentTimeMillis();
        file1.setLastModified(time - 1000); // 1 second ago


        File file2 = new File(validDirectory, "file.criTeria2.txt"); // write 64KB file
        file2.createNewFile();
        
        file2.setLastModified(time - 2000); // 2 seconds ago
        

        when(paramBeanFactory.getDefaultChrootDir()).thenReturn(validDirectory.getPath());

        ImmutableFilesPagingAndFiltering searchConfig = ImmutableFilesPagingAndFiltering.builder()
                                                                                        .filters(Map.of("toRange date", "" + (time - 1500))) // 1.5 seconds ago
                                                                                        .limit(10L)
                                                                                        .offset(0L)
                                                                                        .build();

        Map<String, Object> result = filesApiService.searchFiles(searchConfig);

        assertNotNull(result);
        assertTrue(result.containsKey("total"));
        assertEquals(1L, result.get("total"));
        assertTrue(result.containsKey("limit"));
        assertTrue(result.containsKey("offset"));
        assertTrue(result.containsKey("size"));
        assertTrue(result.containsKey("data"));
        assertNotNull(result.get("data"));
        List<org.meveo.apiv2.admin.File> data = (List<org.meveo.apiv2.admin.File>) result.get("data");
        assertEquals(1, data.size());
        assertEquals("file.criTeria2.txt", data.get(0).getName());
    }

    @Test
    public void shouldNotReturnExclusiveSearchFilesUsingToRangeOnDate() throws IOException {
        File validDirectory = temporaryFolder.newFolder("validDirectory");
        File file1 = new File(validDirectory, "file.criTeria1.txt"); // write 128KB file
        file1.createNewFile();

        long time = System.currentTimeMillis();
        file1.setLastModified(time - 1000); // 1 second ago


        File file2 = new File(validDirectory, "file.criTeria2.txt"); // write 64KB file
        file2.createNewFile();

        file2.setLastModified(time - 2000); // 2 seconds ago


        when(paramBeanFactory.getDefaultChrootDir()).thenReturn(validDirectory.getPath());

        ImmutableFilesPagingAndFiltering searchConfig = ImmutableFilesPagingAndFiltering.builder()
                                                                                        .filters(Map.of("toRange date", "" + (time - 2000))) // 2 seconds ago
                                                                                        .limit(10L)
                                                                                        .offset(0L)
                                                                                        .build();

        Map<String, Object> result = filesApiService.searchFiles(searchConfig);

        assertNotNull(result);
        assertTrue(result.containsKey("total"));
        assertEquals(0L, result.get("total"));
        assertTrue(result.containsKey("limit"));
        assertTrue(result.containsKey("offset"));
        assertTrue(result.containsKey("size"));
        assertTrue(result.containsKey("data"));
        assertNotNull(result.get("data"));
        List<org.meveo.apiv2.admin.File> data = (List<org.meveo.apiv2.admin.File>) result.get("data");
        assertEquals(0, data.size());
    }
}