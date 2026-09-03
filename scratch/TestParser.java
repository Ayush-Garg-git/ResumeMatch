import java.io.*;
import java.util.*;

public class TestParser {
    static class Project {
        String title;
        String role;
        String desc;
        Project(String t, String r, String d) { this.title = t; this.role = r; this.desc = d; }
    }

    public static void main(String[] args) throws Exception {
        // Read text from python extraction of pdf
        Process p = Runtime.getRuntime().exec(new String[]{"python", "-c", "import pypdf; reader = pypdf.PdfReader(r'C:\\Users\\ASUS\\.gemini\\antigravity-ide\\brain\\939f6daa-170c-4ff8-9b79-3a6947bc5512\\.user_uploaded\\media_1788379275225.pdf'); print(''.join(page.extract_text() for page in reader.pages))"});
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        String text = sb.toString();

        System.out.println("Text length: " + text.length());

        List<Project> projects = new ArrayList<>();
        Set<String> existingProjectTitles = new HashSet<>();

        extractDeterministicProjects(projects, text, existingProjectTitles);

        System.out.println("Extracted Projects: " + projects.size());
        for (Project proj : projects) {
            System.out.println("TITLE: " + proj.title);
            System.out.println("ROLE/TECH: " + proj.role);
            System.out.println("DESC: " + proj.desc);
            System.out.println("-------------------------");
        }
    }

    private static void extractDeterministicProjects(List<Project> projects, String text, Set<String> existingProjectTitles) {
        if (text == null || text.isBlank()) return;

        // Clean tabs, non-breaking spaces, and unicode replacement characters
        String cleanText = text.replaceAll("[\\t\\u00A0\\uFFFD]+", " ");
        String[] lines = cleanText.split("\\r?\\n");

        boolean inProjectsSection = false;
        String currentTitle = null;
        String currentTech = null;
        List<String> currentBullets = new ArrayList<>();

        String[] actionVerbs = new String[]{
            "built", "handled", "used", "integrated", "worked", "developed", "created", "designed", "tested", "managed", "deployed"
        };

        for (String rawLine : lines) {
            String trimmed = rawLine.trim().replaceAll(" +", " ");
            if (trimmed.isEmpty()) continue;

            String upper = trimmed.toUpperCase();
            if (upper.equals("PROJECTS") || upper.equals("ACADEMIC PROJECTS") || upper.equals("TECHNICAL PROJECTS")) {
                inProjectsSection = true;
                continue;
            }
            if (inProjectsSection && (upper.startsWith("LEADERSHIP") || upper.startsWith("CERTIFICATION") || 
                                      upper.startsWith("EDUCATION") || upper.startsWith("EXPERIENCE") ||
                                      upper.startsWith("PROFESSIONAL EXPERIENCE") || upper.startsWith("TECHNICAL SKILLS"))) {
                savePendingProject(projects, currentTitle, currentTech, currentBullets, existingProjectTitles);
                inProjectsSection = false;
                break;
            }

            if (inProjectsSection) {
                String lower = trimmed.toLowerCase();
                boolean isBullet = trimmed.startsWith("•") || trimmed.startsWith("-") || trimmed.startsWith("*") || 
                                   trimmed.endsWith(".") || isStartingWithAnyVerb(lower, actionVerbs);
                
                boolean isTech = (trimmed.contains(",") || trimmed.contains(";") || trimmed.contains("/")) &&
                                 (trimmed.contains("Java") || trimmed.contains("Python") || trimmed.contains("IoT") || 
                                  trimmed.contains("API") || trimmed.contains("SQL") || trimmed.contains("Vision") || 
                                  trimmed.contains("Cloud") || trimmed.contains("Sensors") || trimmed.contains("Hardware") || 
                                  trimmed.contains("Spring") || trimmed.contains("React") || trimmed.contains("REST"));

                if (!isBullet && !isTech) {
                    // New project title or secondary title line
                    if (currentTitle == null || !currentBullets.isEmpty()) {
                        if (currentTitle != null) {
                            savePendingProject(projects, currentTitle, currentTech, currentBullets, existingProjectTitles);
                            currentBullets.clear();
                        }
                        currentTitle = trimmed;
                        currentTech = "";
                    } else {
                        // Secondary title line (e.g. "Stapler Labs")
                        currentTitle = currentTitle + " (" + trimmed + ")";
                    }
                } else if (isTech && (currentTech == null || currentTech.isEmpty())) {
                    currentTech = trimmed;
                } else {
                    currentBullets.add(trimmed);
                }
            }
        }

        if (inProjectsSection && currentTitle != null) {
            savePendingProject(projects, currentTitle, currentTech, currentBullets, existingProjectTitles);
        }
    }

    private static boolean isStartingWithAnyVerb(String lowerLine, String[] verbs) {
        for (String verb : verbs) {
            if (lowerLine.startsWith(verb)) return true;
        }
        return false;
    }

    private static void savePendingProject(List<Project> projects, String title, String role, List<String> bullets, Set<String> existingProjectTitles) {
        if (title == null || title.isBlank()) return;
        String cleanTitle = title.trim();
        if (!existingProjectTitles.contains(cleanTitle.toLowerCase())) {
            String desc = bullets != null && !bullets.isEmpty() ? String.join("\n", bullets) : "";
            projects.add(new Project(cleanTitle, role != null ? role.trim() : "", desc));
            existingProjectTitles.add(cleanTitle.toLowerCase());
        }
    }
}
