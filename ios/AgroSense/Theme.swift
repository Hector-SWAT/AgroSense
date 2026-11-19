import SwiftUI

extension Color {
    init(hex: UInt32, alpha: Double = 1) {
        self.init(.sRGB, red: Double((hex >> 16) & 0xFF) / 255, green: Double((hex >> 8) & 0xFF) / 255, blue: Double(hex & 0xFF) / 255, opacity: alpha)
    }
    static let agGreen = Color(hex: 0x176B45)
    static let agGreenDark = Color(hex: 0x145B3B)
    static let agGreenSoft = Color(hex: 0xE8F4ED)
    static let agMuted = Color(hex: 0x68776E)
    static let agLine = Color(hex: 0xDCE6DF)
    static let agBg = Color(hex: 0xF4F7F5)
    static let agDanger = Color(hex: 0xA83434)
    static let agWarn = Color(hex: 0x8A5B08)
}

/// Pares de color RGB para botones (degradado inicio → fin).
enum Rgb {
    static let red = [Color(hex: 0xFF1744), Color(hex: 0xFF8A00)]
    static let green = [Color(hex: 0x00C853), Color(hex: 0x00B8D4)]
    static let blue = [Color(hex: 0x2979FF), Color(hex: 0xD500F9)]
    static let purple = [Color(hex: 0x7C4DFF), Color(hex: 0xFF4081)]
    static let orange = [Color(hex: 0xFF6D00), Color(hex: 0xFFD600)]
    static let teal = [Color(hex: 0x00BFA5), Color(hex: 0x2962FF)]
    static let gray = [Color.gray, Color.gray.opacity(0.6)]
    static let spectrum = [Color(hex: 0xFF0040), Color(hex: 0xFFA000), Color(hex: 0x00E676), Color(hex: 0x00B0FF), Color(hex: 0xD500F9), Color(hex: 0xFF0040)]
}

/// Botón con degradado RGB y borde arcoíris que gira.
struct RgbButton: View {
    let title: String
    var systemImage: String? = nil
    var colors: [Color] = Rgb.green
    var height: CGFloat = 54
    var enabled: Bool = true
    let action: () -> Void
    @State private var angle = 0.0

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if let systemImage { Image(systemName: systemImage) }
                Text(title).fontWeight(.bold).multilineTextAlignment(.center)
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity, minHeight: height)
            .padding(.horizontal, 12)
            .background(LinearGradient(colors: colors, startPoint: .topLeading, endPoint: .bottomTrailing))
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .overlay(
                RoundedRectangle(cornerRadius: 14)
                    .strokeBorder(AngularGradient(colors: Rgb.spectrum, center: .center, angle: .degrees(angle)), lineWidth: 3)
            )
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.45)
        .onAppear { withAnimation(.linear(duration: 3.5).repeatForever(autoreverses: false)) { angle = 360 } }
    }
}

struct BigTile: View {
    let emoji: String, title: String, subtitle: String, colors: [Color]
    @State private var glow = false
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(emoji).font(.system(size: 26)).frame(width: 48, height: 48).background(.white.opacity(0.25)).clipShape(Circle())
            Text(title).font(.headline).foregroundStyle(.white)
            Text(subtitle).font(.caption).foregroundStyle(.white.opacity(0.9))
        }
        .frame(maxWidth: .infinity, minHeight: 128, alignment: .leading)
        .padding(14)
        .background(LinearGradient(colors: colors, startPoint: .topLeading, endPoint: .bottomTrailing))
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .overlay(RoundedRectangle(cornerRadius: 22).strokeBorder(AngularGradient(colors: Rgb.spectrum, center: .center), lineWidth: 2).opacity(glow ? 1 : 0.5))
        .onAppear { withAnimation(.easeInOut(duration: 1.6).repeatForever()) { glow = true } }
    }
}

struct SectionCard<Content: View>: View {
    var title: String? = nil
    var emoji: String? = nil
    @ViewBuilder let content: () -> Content
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let title { Text("\(emoji ?? "") \(title)").font(.headline) }
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 18))
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color.agLine))
    }
}

struct HelpBox: View {
    let text: String
    var warn = false
    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: warn ? "exclamationmark.triangle.fill" : "info.circle.fill")
            Text(text).font(.subheadline)
        }
        .foregroundStyle(warn ? Color.agWarn : Color.agGreen)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(warn ? Color(hex: 0xFFF8E8) : Color.agGreenSoft)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

struct StatCard: View {
    let label: String, value: String, emoji: String
    var color: Color = .agGreen
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("\(emoji)  \(label)").font(.caption).foregroundStyle(Color.agMuted)
            Text(value).font(.title3.weight(.heavy)).foregroundStyle(color).minimumScaleFactor(0.6).lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(.white)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.agLine))
    }
}

struct ChoiceCard: View {
    let emoji: String, title: String
    var subtitle: String? = nil
    let selected: Bool
    var accent: Color = .agGreen
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            VStack(spacing: 2) {
                Text(emoji).font(.system(size: 30))
                Text(title).font(.caption.weight(selected ? .bold : .medium)).multilineTextAlignment(.center).lineLimit(2)
                if let subtitle { Text(subtitle).font(.caption2).foregroundStyle(Color.agMuted).multilineTextAlignment(.center).lineLimit(2) }
            }
            .foregroundStyle(.primary)
            .frame(maxWidth: .infinity, minHeight: 96)
            .padding(6)
            .background(selected ? accent.opacity(0.14) : .white)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .overlay(RoundedRectangle(cornerRadius: 16).stroke(selected ? accent : Color.agLine, lineWidth: selected ? 2.5 : 1))
        }.buttonStyle(.plain)
    }
}

struct ChoiceRow: View {
    let title: String, subtitle: String, trailing: String
    var selected = false
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: selected ? "largecircle.fill.circle" : "circle").foregroundStyle(Color.agGreen)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).fontWeight(.bold)
                    Text(subtitle).font(.caption).foregroundStyle(Color.agMuted)
                }
                Spacer()
                Text(trailing).font(.footnote.weight(.semibold)).foregroundStyle(Color.agGreen).multilineTextAlignment(.trailing)
            }
            .foregroundStyle(.primary)
            .padding(12)
            .background(selected ? Color.agGreen.opacity(0.08) : .white)
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(selected ? Color.agGreen : Color.agLine, lineWidth: selected ? 2 : 1))
        }.buttonStyle(.plain)
    }
}

struct StepIndicator: View {
    let current: Int
    let labels: [String]
    var body: some View {
        HStack {
            ForEach(Array(labels.enumerated()), id: \.offset) { i, l in
                VStack(spacing: 2) {
                    Text(i < current ? "✓" : "\(i + 1)")
                        .font(.footnote.bold())
                        .foregroundStyle(i <= current ? .white : Color.agMuted)
                        .frame(width: 30, height: 30)
                        .background(i == current ? AnyShapeStyle(LinearGradient(colors: Rgb.blue, startPoint: .leading, endPoint: .trailing))
                                    : i < current ? AnyShapeStyle(LinearGradient(colors: Rgb.green, startPoint: .leading, endPoint: .trailing))
                                    : AnyShapeStyle(Color.agLine))
                        .clipShape(Circle())
                    Text(l).font(.system(size: 10)).foregroundStyle(i == current ? Color.agGreen : Color.agMuted)
                }.frame(maxWidth: .infinity)
            }
        }
    }
}

/// Campo numérico que acepta coma o punto decimal.
struct NumberField: View {
    let label: String
    @Binding var text: String
    var suffix: String? = nil
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.caption.weight(.semibold)).foregroundStyle(Color.agMuted)
            HStack {
                TextField("0", text: $text)
                    .keyboardType(.decimalPad)
                    .onChange(of: text) { _, v in
                        let clean = v.replacingOccurrences(of: ",", with: ".").filter { $0.isNumber || $0 == "." }
                        if clean != v { text = clean }
                    }
                if let suffix { Text(suffix).foregroundStyle(Color.agMuted) }
            }
            .padding(12)
            .background(.white)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color(hex: 0xCBD8CF)))
        }
    }
}

extension Double { var nonZero: Double? { self > 0 ? self : nil } }
extension String { var short: String { components(separatedBy: " (").first ?? self } }

let dateFmt: DateFormatter = { let f = DateFormatter(); f.locale = Locale(identifier: "es_MX"); f.dateFormat = "dd/MM/yyyy"; return f }()
let dateTimeFmt: DateFormatter = { let f = DateFormatter(); f.locale = Locale(identifier: "es_MX"); f.dateFormat = "dd/MM/yyyy HH:mm"; return f }()
func money(_ v: Double?) -> String { v.map { "$" + Calculator.fmt($0, 2) } ?? "—" }
