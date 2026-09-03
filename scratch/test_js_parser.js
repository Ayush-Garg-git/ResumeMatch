const text = `
TECHNICAL SKILLS
Hardware:
PC assembly, hardware troubleshooting, BIOS/UEFI, RAM, processors, motherboards, storage devices, peripherals
Networking:
TCP/IP, LAN, DNS, DHCP, cabling, connectivity troubleshooting, ping, ipconfig, tracert, nslookup
Operating Systems:
Linux, Windows, CLI, system configuration, basic troubleshooting
Programming:
Java, C++, Python, JavaScript, TypeScript
Backend & Web:
Spring Boot, Spring MVC, REST APIs, Hibernate/JPA, JDBC, HTML, CSS
Databases:
MySQL, PostgreSQL, SQL
Tools:
Git, GitHub, VS Code, Postman, Node.js, Vercel
PROFESSIONAL EXPERIENCE
Comviva Technologies | Trainee Engineer
Stapler Labs | Full-Stack Engineer
PROJECTS
Prescription Reader \t\t AI Healthcare MVP
Stapler Labs
Cloud Vision, Gemini API, Python, REST APIs
Built and tested an AI-based prescription reader that processes prescription images and extracts medicine details.
Handled image processing, API integration, backend logic, and troubleshooting during testing.
Used Google Cloud Vision as the primary API and Gemini as a fallback for difficult cases.
EyeSonic \t\t IoT Assistive Device
IoT, Sensors, Hardware, Python, Computer Vision
Built an IoT-based assistive device using sensors, hardware components, and audio/haptic feedback.
Handled hardware assembly, sensor connections, device testing, and troubleshooting during development.
Integrated real-time object detection with the device and tested its response in real-world conditions.
Client Websites & Full-Stack Applications
Java, Spring Boot, JavaScript, SQL, REST APIs
Built and deployed full-stack websites based on client requirements.
Handled development, testing, debugging, deployment, and post-feedback fixes.
Worked directly with clients to understand requirements and implement changes.
LEADERSHIP & ACHIEVEMENTS
`;

function extractProjectsFromResumeText(text) {
    if (!text) return [];
    const cleanText = text.replace(/[\t\u00A0\uFFFD]+/g, ' ');
    const lines = cleanText.split(/\r?\n/).map(l => l.trim().replace(/\s+/g, ' ')).filter(Boolean);

    let inProjects = false;
    const projects = [];
    let current = null;

    const actionVerbs = ['built', 'handled', 'used', 'integrated', 'worked', 'developed', 'created', 'designed', 'tested', 'managed', 'deployed'];

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        const upper = line.toUpperCase();

        if (['PROJECTS', 'ACADEMIC PROJECTS', 'TECHNICAL PROJECTS'].includes(upper)) {
            inProjects = true;
            continue;
        }

        if (inProjects && (
            upper.startsWith('LEADERSHIP') || 
            upper.startsWith('CERTIFICATION') || 
            upper.startsWith('EDUCATION') || 
            upper.startsWith('EXPERIENCE') ||
            upper.startsWith('PROFESSIONAL EXPERIENCE') || 
            upper.startsWith('TECHNICAL SKILLS')
        )) {
            if (current) projects.push(current);
            current = null;
            inProjects = false;
            break;
        }

        if (inProjects) {
            const lower = line.toLowerCase();
            const isBullet = line.startsWith('•') || line.startsWith('-') || line.startsWith('*') || 
                             line.endsWith('.') || actionVerbs.some(v => lower.startsWith(v));
            
            const isTech = (line.includes(',') || line.includes(';') || line.includes('/')) &&
                           ['Java', 'Python', 'IoT', 'API', 'SQL', 'Vision', 'Cloud', 'Sensors', 'Hardware', 'Spring', 'React', 'REST'].some(k => line.includes(k));

            if (!isBullet && !isTech) {
                if (!current || current.bullets.length > 0) {
                    if (current) projects.push(current);
                    current = { title: line, role: '', bullets: [] };
                } else {
                    current.title = current.title + ' (' + line + ')';
                }
            } else if (isTech && (!current || !current.role)) {
                if (current) current.role = line;
            } else if (current) {
                current.bullets.push(line);
            }
        }
    }

    if (current) projects.push(current);

    return projects.map(p => ({
        title: p.title,
        role: p.role || '',
        description: p.bullets.join('\n')
    }));
}

const res = extractProjectsFromResumeText(text);
console.log('Total extracted:', res.length);
console.log(JSON.stringify(res, null, 2));
