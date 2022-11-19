using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace ArchieMate.Chatbot.Migrations
{
    public partial class AddChannelForeignKeyToCommand : Migration
    {
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "Channel",
                table: "Commands");

            migrationBuilder.AddColumn<Guid>(
                name: "ChannelId",
                table: "Commands",
                type: "uuid",
                nullable: false,
                defaultValue: new Guid("00000000-0000-0000-0000-000000000000"));

            migrationBuilder.CreateIndex(
                name: "IX_Commands_ChannelId",
                table: "Commands",
                column: "ChannelId");

            migrationBuilder.AddForeignKey(
                name: "FK_Commands_Channels_ChannelId",
                table: "Commands",
                column: "ChannelId",
                principalTable: "Channels",
                principalColumn: "Id",
                onDelete: ReferentialAction.Cascade);
        }

        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropForeignKey(
                name: "FK_Commands_Channels_ChannelId",
                table: "Commands");

            migrationBuilder.DropIndex(
                name: "IX_Commands_ChannelId",
                table: "Commands");

            migrationBuilder.DropColumn(
                name: "ChannelId",
                table: "Commands");

            migrationBuilder.AddColumn<string>(
                name: "Channel",
                table: "Commands",
                type: "text",
                nullable: false,
                defaultValue: "");
        }
    }
}
