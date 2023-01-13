using Serilog;
using ArchieMate.Chatbot;
using System.Diagnostics;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
Log.Logger = new LoggerConfiguration().CreateBootstrapLogger();
builder.Host.UseSerilog(
    ((ctx, lc) => lc
    .ReadFrom.Configuration(ctx.Configuration)));

builder.Services.UseChatbotServices(builder.Configuration);

builder.Services.AddControllers();
// Learn more about configuring Swagger/OpenAPI at https://aka.ms/aspnetcore/swashbuckle
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var app = builder.Build();

try
{
    // Configure the HTTP request pipeline.
    if (app.Environment.IsDevelopment())
    {
        app.UseDeveloperExceptionPage();
        app.UseSwagger();
        app.UseSwaggerUI();
    }

    app.UseSerilogRequestLogging();

    app.UseAuthorization();

    app.UseChatbotEndpoints();

    using (var scope = app.Services.CreateScope())
    {
        // Force DB migrations before staring app
        app.Logger.LogInformation("Performing database migrations...");
        await scope.PerformDatabaseMigrations();
    }

    app.Logger.LogInformation("Starting");

    app.Run();
}
catch (Exception ex)
{
    ex.Demystify();
    string type = ex.GetType().Name;
    if (type.Equals("StopTheHostException", StringComparison.Ordinal)) // Catch the private StopTheHostException
    {
        throw; // And rethrow it so it is handled appropriately
    }
    app.Logger.LogCritical(ex, "Unhandled exception!");
}
finally
{
    app.Logger.LogInformation("FINISHED");
    Log.CloseAndFlush();
}

public partial class Program { }
