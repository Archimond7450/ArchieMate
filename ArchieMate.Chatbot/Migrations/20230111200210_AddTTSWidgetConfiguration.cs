using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ArchieMate.Chatbot.Migrations
{
    /// <inheritdoc />
    public partial class AddTTSWidgetConfiguration : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "TTSWidgetConfigurations",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "uuid", nullable: false),
                    ChannelId = table.Column<Guid>(type: "uuid", nullable: false),
                    Enabled = table.Column<bool>(type: "boolean", nullable: false),
                    OnlyFromMentions = table.Column<bool>(type: "boolean", nullable: false),
                    DelayBetweenMessages = table.Column<long>(type: "bigint", nullable: false),
                    VolumePercent = table.Column<long>(type: "bigint", nullable: false),
                    MaxDuration = table.Column<long>(type: "bigint", nullable: false),
                    AllowEmoteOnly = table.Column<bool>(type: "boolean", nullable: false),
                    Censoring = table.Column<bool>(type: "boolean", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_TTSWidgetConfigurations", x => x.Id);
                });
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "TTSWidgetConfigurations");
        }
    }
}
