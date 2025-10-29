# LogAnalyzer - IntelliJ IDEA Plugin

<!-- Plugin description -->
An IntelliJ IDEA plugin for analyzing Apache/Nginx log files with detailed insights and visualizations.

Analyze Apache/Nginx log files directly in your IDE with powerful insights:
- **Traffic Analysis** - View request patterns by hour with visual charts
- **Status Codes** - Distribution of HTTP response codes with percentages
- **Top Endpoints** - Most frequently accessed URLs
- **Performance Metrics** - Response size analysis and data transfer statistics
- **Security Analysis** - Detect suspicious activity and potential attacks

Perfect for developers and DevOps engineers who need to analyze web server logs during development and debugging.
<!-- Plugin description end -->

## 🚀 Features

- **Traffic Analysis** - Visualize request patterns by hour
- **Status Code Distribution** - Breakdown of HTTP response codes (2xx, 3xx, 4xx, 5xx)
- **Top Endpoints** - Most accessed URLs with request counts
- **Performance Metrics** - Response size analysis and largest endpoints
- **Security Analysis** - Detect suspicious IPs and potential attacks

## 📸 Screenshots
### Overview:
![LogAnalyzer Overview](screenshots/overview.png)

### Traffic:
![Traffic Analysis](screenshots/traffic.png)

## 🔧 Installation

### From Source
1. Clone this repository
2. Run `./gradlew buildPlugin`
3. Install plugin from disk in IntelliJ IDEA: `Settings → Plugins → ⚙️ → Install Plugin from Disk`

## 📖 Usage

1. Right-click on any `.log` file in your project
2. Select **"Analyze Log File"**
3. View detailed analysis in the LogAnalyzer tool window

## 🧪 Testing

Sample log files are provided in `test-logs/` directory.

## 🛠️ Built With

- IntelliJ Platform SDK
- Java 21
- Gradle

## 📝 Log Format Support

Currently supports:
- Apache Combined Log Format
- Nginx Access Logs

## 🚧 Work In Progress

This plugin is under active development. Planned features:
- Export to JSON/HTML reports
- Live log tailing
- Log filtering and search
- Custom threshold configuration

## 📄 License

MIT License - see LICENSE file for details

## 👨‍💻 Author

Matei Gatin - [GitHub](https://github.com/Matei-Gatin)
